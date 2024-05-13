(ns cljaws.aws-client
  (:require [clojure.java.io :as io]
            [clojure.walk :refer [keywordize-keys]]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.client.api.async :as aws.async]
            [cognitect.aws.credentials :as credentials]
            [cljaws.sts :as sts]))

(def ^{:dynamic true :private false} *client* nil)

(defn- error?
  "Checks the given result object, and returns the errors if there are any."
  [results]
  (when (or (pos? (count (:Errors (:Response results))))
            (:Error results))
    results))

(defn- error-message
  "Returns a string from an AWS error result."
  [results]
  (cond (:Response results) (let [error (:Errors (:Response results))]
                              (str (:Code (:Error error)) ": "
                                   (:Message (:Error error))))
        (:Error results)      (str (:Code (:Error results)) ": "
                                   (:Message (:Error results)))
        :else (str {:Code "UndetectedErrorFormat" :Message results})))

(defn- token-expired?
  ([] (token-expired? :default "us-east-1"))
  ([profile] (token-expired? profile "us-east-1"))
  ([profile region]
   (let [sts-client (aws/client {:api :sts
                                 :credentials-provider
                                 (credentials/profile-credentials-provider (name profile))
                                 :region region})
         results (aws/invoke sts-client {:op :GetCallerIdentity})]
     (sts/request-expired? (:ErrorResponse results)))))

(defn create-client
  "Creates an AWS client, for the specified api and profile."
  ([api] (create-client api :default "us-east-1"))
  ([api profile] (create-client api profile "us-east-1"))
  ([api profile region]
   (let [client
         (aws/client
          {:api (keyword api)
           :credentials-provider
           (credentials/profile-credentials-provider (name profile))
           :region region})]

     (aws/validate-requests client true)
     client)))

(defn async
  ([api request] (async api request :default "us-east-1"))
  ([api request profile] (async api request profile "us-east-1"))
  ([api request profile region]
   (try
     (let [client (create-client api profile region)
           results (aws.async/invoke client request)]
       (cond (sts/request-expired? results)   (throw (.Exception "Your AWS Token has expired!"))
             (error? results)  (throw (Exception. (error-message results)))
             :else                   results)))))

(defn awscli
  "Uses a dynamic binding for a client, creates a new client if it does
  not yet exist."
  ([api request] (awscli api request :default "us-east-1"))
  ([api request profile] (awscli api request profile "us-east-1"))
  ([api request profile region]
   (if (token-expired? profile)
     (throw (.Exception "Your AWS Token has expired!"))
     (try
       (if *client*
         (let [results (aws/invoke *client* request)]
           (if (error? results)
             (throw (Exception. (error-message results)))
             results))
         (binding [*client* (create-client api profile region)]
           (awscli api request profile region)))))))
