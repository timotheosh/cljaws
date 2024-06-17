(defproject timotheosh/cljaws "0.5.4-SNAPSHOT"
  :description "Convenience functions for interacting with AWS API's from closh"
  :url "https://github.com/timotheosh/cljaws"
  :license {:name "MIT"
            :url "https://www.opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure                     "1.11.3"]
                 [clojure-term-colors/clojure-term-colors "0.1.0"]
                 [org.ini4j/ini4j                         "0.5.4"]
                 [clj-http/clj-http                       "3.13.0"]
                 [com.cognitect.aws/api                   "0.8.692"]
                 [com.cognitect.aws/endpoints             "1.1.12.718"]
                 [com.cognitect.aws/sts                   "857.2.1574.0"]
                 [com.cognitect.aws/iam                   "868.2.1599.0"]
                 [com.cognitect.aws/s3                    "868.2.1580.0"]
                 [com.cognitect.aws/ec2                   "869.2.1616.0"]
                 [com.cognitect.aws/cloudformation        "868.2.1599.0"]
                 [com.cognitect.aws/autoscaling           "857.2.1574.0"]
                 [com.cognitect.aws/dynamodb              "869.2.1616.0"]
                 [com.cognitect.aws/logs                  "868.2.1584.0"]
                 [com.cognitect.aws/opsworks              "847.2.1365.0"]
                 [com.cognitect.aws/support               "869.2.1616.0"]
                 [com.cognitect.aws/service-quotas        "857.2.1574.0"]
                 [org.clojure/java.jdbc                   "0.7.12"]
                 [org.xerial/sqlite-jdbc                  "3.45.3.0"]
                 [org.clojure/data.csv                    "1.1.0"]]
  :repl-options {:init-ns cljaws.core}
  :profiles {:dev {:plugins [[com.jakemccrary/lein-test-refresh "0.25.0"]]}}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password}]])
