(ns cljaws.aws-client
  (:require [clojure.java.io :as io]
            [clojure.walk :refer [keywordize-keys]]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [cljaws.sts :as sts]))

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

(defn create-client
  "Creates an AWS client, for the specified api and environment."
  ([api] (create-client api :dev "us-east-1"))
  ([api environment] (create-client api environment "us-east-1"))
  ([api environment region]
   (let [client
         (aws/client
          {:api (keyword api)
           :credentials-provider
           (credentials/profile-credentials-provider (name environment))
           :region region})]
     (aws/validate-requests client true)
     client)))

(defn awscli
  ([api request] (awscli api request :dev "us-east-1"))
  ([api request environment] (awscli api request environment "us-east-1"))
  ([api request environment region]
   (try
     (let [client (create-client api environment region)
           results (aws/invoke client request)]
       (cond (sts/request-expired? results)  (do
                                               (sts/update-token-file environment)
                                               (awscli api request environment region))
             (error? results)  (throw (Exception. (error-message results)))
             :else                   results)))))
