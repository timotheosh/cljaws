(ns cljaws.cloudformation
  (:require [cljaws.aws-client :as aws-client]))

(defn get-stack
  ([cluster] (get-stack cluster "default"))
  ([cluster environment]
   (aws-client/awscli :cloudformation {:op :DescribeStacks
                                       :request {:StackName cluster}})))

(defn get-security-group
  ([cluster] (get-security-group cluster "MongoSecurityGroup" "default"))
  ([cluster logical-resource-id environment]
   (:PhysicalResourceId
    (:StackResourceDetail
     (aws-client/awscli :cloudformation {:op :DescribeStackResource
                                         :request {:StackName cluster
                                                   :LogicalResourceId logical-resource-id}}
                        environment)))))
