(ns cljaws.sts
  (:require [clojure.java.io :as io]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [clojure.term.colors :as colors])
  (:import  [org.ini4j Ini
             Config]
            [java.text SimpleDateFormat]
            [java.util TimeZone]
            [java.time ZonedDateTime]))

(comment "This handles generating AWS session tokens using a user's MFA
         device.  It assumes the user is an iam user who can list
         thier MFA devices with their aws_access_key_id and
         aws_secret_key_id, and has very limited access. Further
         access is granted via an AWS session token. This sets the
         ephemeral access session in the ~/.aws/credentials file,
         including the token expiration time, so new keys can be
         auto-requested.")

;; Our credentials files have iso-formated dates to indicate when
;; session tokens expire. ini4j likes to escape colons since colons
;; delineate key/value pairs. This will keep ini4j from escaping them.
(.setEscape (Config/getGlobal) false)

;; The default location where credentials may be found.
(def default-creds (str (System/getenv "HOME") "/.aws/credentials"))

;; This should probably never change, as AWS has not (yet) deployed
;; its token services to other regions?
(def default-region "us-east-1")

;; Not true iso format, but close enough.
(def iso-format (new SimpleDateFormat "yyyy-MM-dd HH:mm+00:00"))
(.setTimeZone iso-format (TimeZone/getTimeZone "UTC"))

(defn- date->java
  "Convert text date to java.util.Date"
  [date-text]
  (when-not (nil? date-text)
    (.parse iso-format date-text)))

(defn- java->date
  "Convert java.util.Date to text."
  [date]
  (when-not (nil? date)
    (.format iso-format date)))

(defn- epoch
  "Takes java.util.Date and converts to epoch."
  [date]
  (when date
    (.getTime date)))

(defn- error?
  "Checks the given result object, and returns the errors if there are any."
  [results]
  (when (or (pos? (count (:Errors (:Response results))))
            (:Error results)
            (:ErrorResponse results))
    results))

(defn- error-message
  "Returns a string from an AWS error result."
  [results]
  (cond (:Response results) (let [error (:Errors (:Response results))]
                              (str (:Code (:Error error)) ": "
                                   (:Message (:Error error))))
        (:Error results)      (str (:Code (:Error results)) ": "
                                   (:Message (:Error results)))
        (:ErrorResponse results) (str (:Code (:Error (:ErrorResponse results)))
                                      ": "
                                      (:Message (:Error (:ErrorResponse results))))
        :else (str {:Code "UndetectedErrorFormat" :Message results})))

(defn request-expired?
  "Returns true if the error message is due to RequestExpired or need to update session tokens."
  [results]
  (= (:Code (:Error (:Errors (:Response results)))) "RequestExpired"))

(defn invalid-token?
  "Returns true if MFA entry was invalid."
  [results]
  (= (:Message (:Error (:ErrorResponse results)))
     "MultiFactorAuthentication failed with invalid MFA one time pass code. "))

(defn- read-credentials-file
  "Reads a given credentials file, and returns it as an Ini (ini4j) object."
  ([] (read-credentials-file default-creds))
  ([cred-file]
   (new Ini (io/file cred-file))))

(defn- create-root-client
  "Root user account, contrary to the name, has very limited access."
  ([api] (create-root-client api :dev default-region default-creds))
  ([api profile] (create-root-client api profile default-region default-creds))
  ([api profile region] (create-root-client api profile region default-creds))
  ([api profile region cred-file]
   (let [creds (read-credentials-file cred-file)
         access-key (.get creds (name profile) "root_aws_access_key_id")
         secret-key (.get creds (name profile) "root_aws_secret_access_key")
         client
         (aws/client
          {:api (keyword api)
           :credentials-provider
           (credentials/basic-credentials-provider
            {:access-key-id access-key
             :secret-access-key secret-key})
           :region region})]
     (aws/validate-requests client true)
     client)))

(defn- root-client-ops
  "This creates AWS client using the user's static (and limited) AWS
  keys. We need this to query the serial numbers for registered MFA
  devices."
  ([api request] (root-client-ops api request :dev default-region default-creds))
  ([api request profile] (root-client-ops api request profile default-region default-creds))
  ([api request profile region] (root-client-ops api request profile region default-creds))
  ([api request profile region cred-file]
   (try
     (let [client (create-root-client api (name profile) region cred-file)
           results (aws/invoke client request)]
       (when (error? results)
         ;;(throw (Exception. (error-message results)))
         results)
       results))))

(defn- get-mfa
  "Returns a list of registered MFA serial numbers."
  ([] (get-mfa :dev default-region default-creds))
  ([profile] (get-mfa profile default-region default-creds))
  ([profile region] (get-mfa profile region default-creds))
  ([profile region cred-file]
   (root-client-ops :iam {:op :ListMFADevices} profile region cred-file)))

(defn- get-session-token
  "Returns the results from a session token request."
  ([token] (get-session-token token :dev default-region default-creds))
  ([token profile] (get-session-token token profile default-region default-creds))
  ([token profile region] (get-session-token token profile region default-creds))
  ([token profile region cred-file]
   (let [mfa-serial (:SerialNumber
                     (first (:MFADevices (get-mfa profile region cred-file))))]
     (root-client-ops
      :sts
      {:op :GetSessionToken
       :request
       {:DurationSeconds 43200
        :SerialNumber mfa-serial
        :TokenCode token}}
      profile region cred-file))))

(defn- update-credentials-file
  "Updates the credentials file with the new ephemeral session token."
  ([data profile] (update-credentials-file data profile default-creds))
  ([data profile cred-file]
   (.setTimeZone iso-format (TimeZone/getTimeZone "UTC"))
   (let [ini-file (read-credentials-file cred-file)
         sec (name profile)]
     (.put ini-file sec "aws_access_key_id"
           (:AccessKeyId (:Credentials data)))
     (.put ini-file sec "aws_secret_access_key"
           (:SecretAccessKey (:Credentials data)))
     (.put ini-file sec "aws_session_token"
           (:SessionToken (:Credentials data)))
     (.put ini-file sec "expire"
           (java->date (:Expiration (:Credentials data))))
     (.put ini-file sec "aws_security_token"
           (:SessionToken (:Credentials data)))
     (.store ini-file)
     (println ))))

(defn- change-active-profile
  "Changes the 'active' profile, by making it the 'default'."
  ([profile] (change-active-profile profile default-creds))
  ([profile cred-file]
   (let [data (read-credentials-file cred-file)
         active (into {} (get data profile))]
     (.remove data "default")
     (.add data "default")
     (.putAll (.get data "default") active)
     (.store data))))

(defn- update-token
  ([profile] (update-token profile default-creds))
  ([profile cred-file]
   (print (str "[" (colors/cyan (name profile))  "]  AWS MFA Token: "))
   (let [mfa-input (read-line)
         results (get-session-token mfa-input profile
                                    default-region cred-file)]
     (if (invalid-token? results)
       (do
         (print (colors/red "Invalid MFA entry!.  "))
         (println (colors/cyan "Try again!"))
         (update-token profile cred-file))
       (update-credentials-file results profile cred-file)))))

(defn update-token-file
  "Updates credentials file with requested session token, and if it has
  not yet expired, it makes the requested profile the default one
  within the credentials file."
  ([profile] (update-token-file profile default-creds))
  ([profile cred-file]
   (let [creds (read-credentials-file cred-file)
         expire (epoch (date->java (.get creds (name profile) "expire")))
         now (System/currentTimeMillis)]
     (if (or (nil? expire)
             (<= expire now))
       (update-token profile cred-file)
       (do
         (change-active-profile profile cred-file)
         (println (str "Set Active Profile: " (colors/cyan profile) " Expires: "
                       (colors/green (.get creds profile "expire")))))))))
