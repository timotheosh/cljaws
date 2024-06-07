(ns cljaws.aws-client
  (:require [cognitect.aws.client.api :as aws]
            [cognitect.aws.client.api.async :as aws.async]
            [cognitect.aws.credentials :as credentials]
            [cljaws.sts :as sts]))

(comment
  "This namespace is for creating AWS clients. A dynamic bind is possible on
  *client*, enabling the user to make multiple AWS calls using the sdame client,
  rather than recreating the AWS client for each operation. For example:"
  (binding [*client* (create-client :dynamodb :endpoint-override {:protocol :http
                                                                  :hostname "localhost"
                                                                  :port 8002})]
    (awscli :dynamodb {:op :CreateTable
                       :request {:TableName "jira-account-map"
                                 :KeySchema
                                 [{:AttributeName "AccountId" :KeyType "HASH"}
                                  {:AttributeName "Email" :KeyType "RANGE"}]
                                 :AttributeDefinitions
                                 [{:AttributeName "AccountId" :AttributeType "S"}
                                  {:AttributeName "Email" :AttributeType "S"}]
                                 :ProvisionedThroughput
                                 {:ReadCapacityUnits 1, :WriteCapacityUnits 1}}})
    (awscli :dynamodb {:op :ListTables :request {}}))
  )

(def ^{:dynamic true :private false} *client* nil)

(defn error?
  "Checks the given result object, and returns the errors if there are any."
  [results]
  (when (or (pos? (count (:Errors (:Response results))))
            (:Error results))
    results))

(defn validate-response
  "Adds special handling for common and specific error results, if needed, otherwise returns the response as is."
  [response]
  (cond (sts/request-expired? response) (throw (ex-info "Your AWS Token has expired!" {:response response}))
        (error? response) (throw (ex-info "ERROR" {:message response}))
        :else response))

(defn create-client
  "Creates an AWS client, for the specified api and profile. Applies
  validate-requests = true before returning the client."
  [api & {:keys [profile region endpoint-override]}]
  (let [region (or region "us-east-1")
        client (->> {:api (keyword api)
                     :region region}
                    ((fn [x]
                       (if profile
                         (assoc x :credentials-provider (credentials/profile-credentials-provider (name profile)))
                         x)))
                    ((fn [x]
                       (if endpoint-override
                         (assoc x :endpoint-override endpoint-override)
                         x)))
                    aws/client)]
    client))

(defn async
  "Uses a dynamic binding for a client, creates a new client if it does
  not yet exist."
  [api request & {:keys [profile region endpoint-override]}]
  (let [region (or region "us-east-1")]
    (try
      (if *client*
        (validate-response (aws.async/invoke *client* request))
        (binding [*client* (create-client api profile region)]
          (async api request profile region))))))

(defn awscli
  "Uses a dynamic binding for a client, creates a new client if it does
  not yet exist."
  [api request & {:keys [profile region endpoint-override]}]
  (let [region (or region "us-east-1")]
    (try
      (if *client*
        (validate-response (aws/invoke *client* request))
        (binding [*client* (create-client api profile region)]
          (awscli api request profile region))))))
