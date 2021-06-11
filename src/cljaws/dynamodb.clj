(ns cljaws.dynamodb
  (:require [cognitect.aws.client.api :as aws]
            [cljaws.aws-client :as aws-client]
            [cljaws.config :refer [get-env get-region]]))


(defn scan-table
  "Returns a list of all items in a DynamoDB table"
  ([table-name] (scan-table table-name :dev (get-region :dev)))
  ([table-name environment] (scan-table table-name environment (get-region environment)))
  ([table-name environment region]
   (aws-client/awscli
    :dynamodb
    {:op :Scan
     :request {:TableName table-name}}
    environment region)))
