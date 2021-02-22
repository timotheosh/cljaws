(ns cljaws.dynamodb
  (:require [cognitect.aws.client.api :as aws]
            [cljaws.aws-client :as aws-client]))


(defn scan-table
  "Returns a list of all items in a DynamoDB table"
  ([table-name] (scan-table table-name :dev "us-east-1"))
  ([table-name environment] (scan-table table-name environment "us-east-1"))
  ([table-name environment region]
   (aws-client/awscli
    :dynamodb
    {:op :Scan
     :request {:TableName table-name}}
    environment region)))
