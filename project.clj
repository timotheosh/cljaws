(defproject cljaws "0.3.2-SNAPSHOT"
  :description "Convenience functions for interacting with AWS API's from closh"
  :url "https://github.com/timotheosh/cljaws"
  :license {:name "MIT"
            :url "https://www.opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure         "1.10.1"]
                 [clojure-term-colors         "0.1.0"]
                 [org.ini4j/ini4j             "0.5.4"]
                 [com.cognitect.aws/api       "0.8.391"]
                 [com.cognitect.aws/endpoints "1.1.11.670"]
                 [com.cognitect.aws/sts       "770.2.568.0"]
                 [com.cognitect.aws/iam       "770.2.568.0"]
                 [com.cognitect.aws/s3        "770.2.568.0"]
                 [com.cognitect.aws/ec2       "770.2.568.0"]
                 [com.cognitect.aws/cloudformation "770.2.568.0"]
                 [com.cognitect.aws/autoscaling "770.2.568.0"]
                 [com.cognitect.aws/dynamodb  "770.2.568.0"]
                 [com.cognitect.aws/logs      "770.2.568.0"]
                 [com.cognitect.aws/opsworks  "770.2.568.0"]
                 [com.cognitect.aws/support   "770.2.568.0"]
                 [com.cognitect.aws/service-quotas "770.2.568.0"]
                 [org.clojure/java.jdbc       "0.7.10"]
                 [org.xerial/sqlite-jdbc      "3.28.0"]
                 [org.clojure/data.csv "0.1.4"]]
  :repl-options {:init-ns cljaws.core})
