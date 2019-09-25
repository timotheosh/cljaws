(ns cljaws.ec2
  (:require [clojure.string :as str]
            [cognitect.aws.client.api :as aws]
            [cljaws.aws-client :as aws-client]
            [cljaws.datetime :as datetime]
            [cljaws.config :refer [accountid get-env get-region]]))

(defn get-all-instances
  "Returns a list of all instances in a given region and environment."
  ([] (get-all-instances :dev "us-east-1"))
  ([environment] (get-all-instances environment "us-east-1"))
  ([environment region]
   (aws-client/awscli
    :ec2
    {:op :DescribeInstances}
    environment region)))

(defn- ec2-search
  "Returns search results with a given operation and list of filters."
  ([op filters] (ec2-search op filters :dev "us-east-1"))
  ([op filters environment] (ec2-search op filters environment "us-east-1"))
  ([op filters environment region]
   (aws-client/awscli
    :ec2
    {:op op
     :request
     {:Filters
      filters}}
    environment
    region)))

(defn- get-tag-value
  "Returns the value of a given tag's key, using a list of :TagSet"
  [key-name tag-set]
  (loop [[tag & body] tag-set]
    (if (= key-name (:Key tag))
      (:Value tag)
      (when (not (nil? body))
        (recur body)))))

(defn search-eni
  "Returns for enis with given a list of filters."
  ([filters] (search-eni filters :dev "us-east-1"))
  ([filters environment] (search-eni filters environment "us=east-1"))
  ([filters environment region]
   (:NetworkInterfaces
    (ec2-search :DescribeNetworkInterfaces filters environment region))))

(defn search-snapshots
  "Returns a list of ebs snapshots given a list of filters."
  ([filters] (search-eni filters :dev "us-east-1"))
  ([filters environment] (search-eni filters environment "us=east-1"))
  ([filters environment region]
   (:Snapshots
    (ec2-search :DescribeSnapshots filters environment region))))

(defn search-ec2
  "Returns a list of ec2 instances given a list of filters."
  ([filters] (search-ec2 filters :dev "us-east-1"))
  ([filters environment] (search-ec2 filters environment "us-east-1"))
  ([filters environment region]
   (ec2-search :DescribeInstances filters environment region)))

(defn get-by-role
  "Returns search results of ec2 instances by role tag."
  ([role] (get-by-role role :dev "us-east-1"))
  ([role environment] (get-by-role role environment "us-east-1"))
  ([role environment region]
   (ec2-search :DescribeInstances [{:Name "tag:role" :Values [role]}] environment region)))

(defn get-data
  [data-key search-results]
  (map (fn [x]
         ((keyword data-key) (first (:Instances x))))
       (:Reservations search-results)))

(defn get-roles
  [search-results]
  (map (fn [x]
         (:Value
          (first
           (filter #(= (:Key %) "role")
                   (:Tags (first (:Instances x)))))))
       (:Reservations search-results)))

(defn get-ip
  "Returns a list of private ip addresses from search results."
  [search-results]
  (get-data :PrivateIpAddress search-results))

(defn get-all-snapshots
  "Returns a list of an account's active snapshots"
  ([] (get-all-snapshots :dev))
  ([env]
   (let [acc (accountid env)
         snaps (:Snapshots
                (aws-client/awscli :ec2 {:op :DescribeSnapshots}
                                   (get-env env) (get-region env)))]
     (filter #(= (:OwnerId %) acc) snaps))))

(defn count-snaps
  "Returns the number of snapshots in environment."
  [env]
  (let [acc (accountid env)
        snaps (:Snapshots
               (aws-client/awscli :ec2 {:op :DescribeSnapshots}
                                  (get-env env) (get-region env)))]
    (count (filter #(= (:OwnerId %) acc) snaps))))
