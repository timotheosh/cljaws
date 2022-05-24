(ns cljaws.dynamodb
  (:require [cognitect.aws.client.api :as aws]
            [cljaws.aws-client :as aws-client]
            [cljaws.config :refer [get-env get-region]]))


(defn scan-table
  "Returns a list of all items in a DynamoDB table"
  ([table-name] (scan-table table-name :dev (get-region :dev)))
  ([table-name habitat] (scan-table table-name habitat (get-region habitat)))
  ([table-name habitat region]
   (aws-client/awscli
    :dynamodb
    {:op :Scan
     :request {:TableName table-name}}
    habitat region)))
