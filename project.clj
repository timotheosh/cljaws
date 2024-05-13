(defproject timotheosh/cljaws "0.4.0-SNAPSHOT"
  :description "Convenience functions for interacting with AWS API's from closh"
  :url "https://github.com/timotheosh/cljaws"
  :license {:name "MIT"
            :url "https://www.opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure                     "1.11.1"]
                 [clojure-term-colors/clojure-term-colors "0.1.0"]
                 [org.ini4j/ini4j                         "0.5.4"]
                 [clj-http/clj-http                       "3.12.3"]
                 [com.cognitect.aws/api                   "0.8.692"]
                 [com.cognitect.aws/endpoints             "1.1.12.626"]
                 [com.cognitect.aws/sts                   "847.2.1387.0"]
                 [com.cognitect.aws/iam                   "848.2.1413.0"]
                 [com.cognitect.aws/s3                    "848.2.1413.0"]
                 [com.cognitect.aws/ec2                   "848.2.1413.0"]
                 [com.cognitect.aws/cloudformation        "848.2.1400.0"]
                 [com.cognitect.aws/autoscaling           "852.2.1525.0"]
                 [com.cognitect.aws/dynamodb              "848.2.1413.0"]
                 [com.cognitect.aws/logs                  "848.2.1413.0"]
                 [com.cognitect.aws/opsworks              "847.2.1365.0"]
                 [com.cognitect.aws/support               "847.2.1387.0"]
                 [com.cognitect.aws/service-quotas        "847.2.1365.0"]
                 [org.clojure/java.jdbc                   "0.7.12"]
                 [org.xerial/sqlite-jdbc                  "3.45.1.0"]
                 [org.clojure/data.csv                    "1.1.0"]]
  :repl-options {:init-ns cljaws.core})
