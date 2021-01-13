(defproject timotheosh/cljaws "0.3.2-SNAPSHOT"
  :description "Convenience functions for interacting with AWS API's from closh"
  :url "https://github.com/timotheosh/cljaws"
  :license {:name "MIT"
            :url "https://www.opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure         "1.10.1"]
                 [clojure-term-colors         "0.1.0"]
                 [org.ini4j/ini4j             "0.5.4"]
                 [clj-http                    "3.10.3"]
                 [com.cognitect.aws/api       "0.8.474"]
                 [com.cognitect.aws/endpoints "1.1.11.842"]
                 [com.cognitect.aws/sts       "798.2.678.0"]
                 [com.cognitect.aws/iam       "801.2.704.0"]
                 [com.cognitect.aws/s3        "809.2.734.0"]
                 [com.cognitect.aws/ec2       "809.2.734.0"]
                 [com.cognitect.aws/cloudformation "801.2.706.0"]
                 [com.cognitect.aws/autoscaling "807.2.729.0"]
                 [com.cognitect.aws/dynamodb  "799.2.679.0"]
                 [com.cognitect.aws/logs      "798.2.672.0"]
                 [com.cognitect.aws/opsworks  "770.2.568.0"]
                 [com.cognitect.aws/support   "801.2.700.0"]
                 [com.cognitect.aws/service-quotas "770.2.568.0"]
                 [org.clojure/java.jdbc       "0.7.11"]
                 [org.xerial/sqlite-jdbc      "3.32.3.2"]
                 [org.clojure/data.csv "1.0.0"]]
  :repl-options {:init-ns cljaws.core})
