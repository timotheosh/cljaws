(ns cljaws.opsworks
  (:require [clojure.string :as str]
            [cognitect.aws.client.api :as aws]
            [cljaws.aws-client :as aws-client]
            [cljaws.config :refer [accountid get-env get-region]]))

(defn get-stacks
  "Returns all the opsworks stacks in given environment."
  [env]
  (into {}
        (map
         (fn [x] {(keyword (:Name x)) (:StackId x)})
         (:Stacks
          (aws-client/awscli :opsworks
                             {:op :DescribeStacks} (get-env env) (get-region env))))))

(defn describe-layers
  "Describe layers of a given stack."
  [stack-name env]
  (binding [aws-client/*client*
            (aws-client/create-client :opsworks (get-env env) (get-region env))]
    (let [stack-id ((keyword stack-name) (get-stacks env))]
      (aws-client/awscli :opsworks {:op :DescribeLayers
                                    :request {
                                              :StackId stack-id}}))))
(defn describe-instances
  "Describe instances of a given stack."
  [stack-name env]
  (binding [aws-client/*client*
            (aws-client/create-client :opsworks (get-env env) (get-region env))]
    (let [stack-id ((keyword stack-name) (get-stacks env))]
      (aws-client/awscli :opsworks {:op :DescribeInstances
                                    :request {
                                              :StackId stack-id}}))))
