(defproject timotheosh/cljaws "0.5.5"
  :description "Convenience functions for interacting with AWS API's from closh"
  :url "https://github.com/timotheosh/cljaws"
  :license {:name "MIT"
            :url "https://www.opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure                     "1.12.5"]
                 [clojure-term-colors/clojure-term-colors "0.1.0"]
                 [org.ini4j/ini4j                         "0.5.4"]
                 [clj-http/clj-http                       "3.13.1"]
                 [com.cognitect.aws/api                   "0.8.824"]
                 [com.cognitect.aws/endpoints             "871.2.44.4"]
                 [com.cognitect.aws/sts                   "871.2.42.37"]
                 [com.cognitect.aws/iam                   "871.2.44.1"]
                 [com.cognitect.aws/s3                    "871.2.43.0"]
                 [com.cognitect.aws/ec2                   "871.2.44.4"]
                 [com.cognitect.aws/cloudformation        "871.2.39.3"]
                 [com.cognitect.aws/autoscaling           "871.2.42.37"]
                 [com.cognitect.aws/dynamodb              "871.2.42.19"]
                 [com.cognitect.aws/logs                  "871.2.44.1"]
                 [com.cognitect.aws/opsworks              "871.2.29.35"]
                 [com.cognitect.aws/support               "871.2.32.30"]
                 [com.cognitect.aws/service-quotas        "871.2.40.9"]
                 [org.clojure/java.jdbc                   "0.7.12"]
                 [org.xerial/sqlite-jdbc                  "3.53.1.0"]
                 [org.clojure/data.csv                    "1.1.1"]]
  :repl-options {:init-ns cljaws.core}
  :profiles {:dev {:plugins [[com.jakemccrary/lein-test-refresh "0.25.0"]]}}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])