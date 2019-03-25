(ns cljaws.autoscaling
  (:require [cljaws.aws-client :as aws-client]
            [cognitect.aws.client.api :as aws]
            [clojure.java.jdbc :as jdbc]))

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     (str (System/getenv "HOME") "/.closh/asg_list.db")
   })

(defn- create-db
  "create db and table"
  [profile]
  (try
    (jdbc/db-do-commands db (jdbc/drop-table-ddl (keyword profile)))
    (catch Exception e
      (println  "table does not, yet exist")))
  (try
    (jdbc/db-do-commands
     db
     (jdbc/create-table-ddl
      (keyword profile)
      [[:id :integer :primary :key]
       [:name :text]]))))

(defn print-result-set
  "prints the result set in tabular form"
  [result-set]
  (doseq [row result-set]
    (println row)))

(defn- search-data
  [asg-name data]
  (let [a-name (:AutoScalingGroupName data)]
    (println a-name)))

(defn- query-asg
  [token]
  (if token
    (aws-client/awscli :autoscaling {:op :DescribeAutoScalingGroups
                                     :request {:NextToken token}})
    (aws-client/awscli :autoscaling {:op :DescribeAutoScalingGroups})))


(defn search-asg
  "Search for ASG."
  ([profile asg-name] (search-asg profile asg-name nil))
  ([profile asg-name next-token]
   (let [results (query-asg next-token)
         groups (:AutoScalingGroups results)
         token (:NextToken results)]
     (map (fn [x] (search-data asg-name x)) groups)
     (if token
       (search-asg profile asg-name token)
       results))))
