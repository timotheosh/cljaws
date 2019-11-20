(ns cljaws.cloudformation
  (:require [clojure.string :as str]
            [cognitect.aws.client.api :as aws]
            [cljaws.aws-client :as aws-client]
            [cljaws.config :refer [accountid get-env get-region]]))

;; Many AWS API calls depend on others. For instance, if we wanted to
;; get a list of resources for a Cloudformation stack, we have to know
;; the stack name we want to get the list of resources for. If we
;; wanted to find all stacks that reference a specific resource, we
;; have to first get a list of all stacks, and then query each stack
;; for a list of referenced resources, to see if the given stack
;; references the resource. If we have 600 stacks, then this query
;; will require 600 independent queries to AWS.
;;
;; We need to avoid getting rate limited, and we need to keep track of
;; what stacks referenced the given resource.
;;
;; To do this, we use doseq, instead of map, and add items to a global
;; atom until all the stacks have been queried, sleeping for half a
;; second in between API calls, as to avoid getting rate limited.
(def stacklist (atom []))

(defn get-stack
  ([cluster] (get-stack cluster "default"))
  ([cluster environment]
   (aws-client/awscli :cloudformation {:op :DescribeStacks
                                       :request {:StackName cluster}})))

(defn get-stacks
  "Single query to get a list of stacks."
  ([env] (get-stacks env nil))
  ([env next-token]
   (if next-token
     (aws-client/awscli :cloudformation {:op :DescribeStacks
                                         :request {:NextToken next-token}}
                        (get-env env) (get-region env))
     (aws-client/awscli :cloudformation {:op :DescribeStacks}
                        (get-env env) (get-region env)))))

(defn get-all-stacks
  "Returns a list of all stacks."
  ([] (get-all-stacks :dev))
  ([env]
   (binding [aws-client/*client* (aws-client/create-client
                                  :cloudformation (get-env env) (get-region env))]
     (loop [stacks (get-stacks env nil)
            result []]
       (if-not (:NextToken stacks)
         result
         (recur (get-stacks env (:NextToken stacks))
                (concat result (:Stacks stacks))))))))

(defn get-all-stack-names
  "Returns a list of all stack names"
  ([] (get-all-stack-names :dev))
  ([env]
   (let [all-stacks (get-all-stacks env)]
     (mapv #(:StackName %) all-stacks))))

(defn get-stack-resources
  "Returns stack resources as a list."
  [stack-name env]
  (:StackResourceSummaries
   (aws-client/awscli :cloudformation
                      {:op :ListStackResources
                       :request
                       {:StackName stack-name}} (get-env env) (get-region env))))

(defn- doseq-interval
  "doseq with an interval. Typical usage is to slow down AWS API calls
  in order to avoid getting rate-limited."
  [f coll interval]
  (doseq [x coll]
    (Thread/sleep interval)
    (f x)))

(defn has-opsworks?
  "Returns true if stack has opsworks as a resource."
  [stack-name env]
  (some #(str/starts-with? % "AWS::OpsWorks")
        (map #(:ResourceType %) (get-stack-resources stack-name env))))

(defn add-stack
  "Modifies global atom, adding the stack-name if the stack is true
  according to the given predicate."
  [pred env stack-name]
  (when (pred stack-name env)
    (reset! stacklist (conj @stacklist stack-name))))

(defn get-opsworks-stacks
  "Return a list of stacs that reference OpsWorks from a given vector of stacks."
  [env]
  (reset! stacklist [])
  (binding [aws-client/*client* (aws-client/create-client :cloudformation
                                                          (get-env env)
                                                          (get-region env))]
    (let [stacks (get-all-stack-names env)]
      (doseq-interval (partial add-stack has-opsworks? env) stacks 500)))
  @stacklist)

(defn get-security-group
  ([cluster] (get-security-group cluster "MongoSecurityGroup" "default"))
  ([cluster logical-resource-id environment]
   (:PhysicalResourceId
    (:StackResourceDetail
     (aws-client/awscli
      :cloudformation {:op :DescribeStackResource
                       :request {:StackName cluster
                                 :LogicalResourceId logical-resource-id}}
      environment)))))
