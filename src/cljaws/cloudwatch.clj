(ns cljaws.cloudwatch
  (:require [cognitect.aws.client.api :as aws]
            [cljaws.aws-client :as aws-client]))

(defn get-log-ops
  "Quick query to get a list of available operations on CloudWatch logs."
  ([] (get-log-ops :dev "us-east-1"))
  ([environment] (get-log-ops environment "us-east-1"))
  ([environment region]
   (keys (aws/ops (aws-client/create-client :logs environment region)))))

;; testing with /aws/lambda/serverless-node-hello
(defn list-subscription-filters
  "Get a list of subscription filters of a given CloudWatch log group name."
  ([group-name] (list-subscription-filters group-name :dev "us-east-1"))
  ([group-name environment] (list-subscription-filters group-name environment "us-east-1"))
  ([group-name environment region]
   (aws-client/awscli
    :logs
    {:op :DescribeSubscriptionFilters
     :request
     {:logGroupName group-name}}
    environment region)))
