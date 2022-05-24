(ns cljaws.cloudwatch
  (:require [cognitect.aws.client.api :as aws]
            [cljaws.aws-client :as aws-client]))

(defn get-log-ops
  "Quick query to get a list of available operations on CloudWatch logs."
  ([] (get-log-ops :dev "us-east-1"))
  ([habitat] (get-log-ops habitat "us-east-1"))
  ([habitat region]
   (keys (aws/ops (aws-client/create-client :logs habitat region)))))

;; testing with /aws/lambda/serverless-node-hello
(defn list-subscription-filters
  "Get a list of subscription filters of a given CloudWatch log group name."
  ([group-name] (list-subscription-filters group-name :dev "us-east-1"))
  ([group-name habitat] (list-subscription-filters group-name habitat "us-east-1"))
  ([group-name habitat region]
   (aws-client/awscli
    :logs
    {:op :DescribeSubscriptionFilters
     :request
     {:logGroupName group-name}}
    habitat region)))
