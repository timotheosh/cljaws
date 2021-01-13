(defproject timotheosh/cljaws "0.3.3-SNAPSHOT"
  :description "Convenience functions for interacting with AWS API's from closh"
  :url "https://github.com/timotheosh/cljaws"
  :license {:name "MIT"
            :url "https://www.opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure         "1.10.1"]
                 [clojure-term-colors         "0.1.0"]
                 [org.ini4j/ini4j             "0.5.4"]
                 [clj-http                    "3.11.0"]
                 [com.cognitect.aws/api       "0.8.498"]
                 [com.cognitect.aws/endpoints "1.1.11.934"]
                 [com.cognitect.aws/sts       "809.2.784.0"]
                 [com.cognitect.aws/iam       "801.2.704.0"]
                 [com.cognitect.aws/s3        "810.2.817.0"]
                 [com.cognitect.aws/ec2       "810.2.817.0"]
                 [com.cognitect.aws/cloudformation "810.2.801.0"]
                 [com.cognitect.aws/autoscaling "811.2.824.0"]
                 [com.cognitect.aws/dynamodb  "810.2.801.0"]
                 [com.cognitect.aws/logs      "809.2.784.0"]
                 [com.cognitect.aws/opsworks  "770.2.568.0"]
                 [com.cognitect.aws/support   "801.2.700.0"]
                 [com.cognitect.aws/service-quotas "810.2.817.0"]
                 [org.clojure/java.jdbc       "0.7.11"]
                 [org.xerial/sqlite-jdbc      "3.34.0"]
                 [org.clojure/data.csv "1.0.0"]]
  :repl-options {:init-ns cljaws.core})
