(defproject cljaws "0.3.2-SNAPSHOT"
  :description "Convenience functions for interacting with AWS API's from closh"
  :url "https://github.com/timotheosh/cljaws"
  :license {:name "MIT"
            :url "https://www.opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure         "1.10.1"]
                 [clojure-term-colors         "0.1.0"]
                 [org.ini4j/ini4j             "0.5.4"]
                 [com.cognitect.aws/api       "0.8.352"]
                 [com.cognitect.aws/endpoints "1.1.11.636"]
                 [com.cognitect.aws/sts       "741.2.504.0"]
                 [com.cognitect.aws/iam       "746.2.533.0"]
                 [com.cognitect.aws/s3        "726.2.488.0"]
                 [com.cognitect.aws/ec2       "746.2.533.0"]
                 [com.cognitect.aws/cloudformation "746.2.533.0"]
                 [com.cognitect.aws/autoscaling "746.2.533.0"]
                 [com.cognitect.aws/dynamodb  "746.2.533.0"]
                 [com.cognitect.aws/logs      "738.2.501.0"]
                 [com.cognitect.aws/opsworks  "746.2.533.0"]
                 [com.cognitect.aws/support   "747.2.533.0"]
                 [com.cognitect.aws/service-quotas "746.2.533.0"]
                 [org.clojure/java.jdbc       "0.7.10"]
                 [org.xerial/sqlite-jdbc      "3.28.0"]
                 [org.clojure/data.csv "0.1.4"]]
  :repl-options {:init-ns cljaws.core})
