(defproject cljaws "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure         "1.10.0"]
                 [com.cognitect.aws/api       "0.8.273"]
                 [com.cognitect.aws/endpoints "1.1.11.507"]
                 [com.cognitect.aws/s3        "697.2.391.0"]
                 [com.cognitect.aws/ec2       "711.2.413.0"]
                 [com.cognitect.aws/cloudformation "697.2.391.0"]
                 [com.cognitect.aws/dynamodb  "697.2.391.0"]]
  :repl-options {:init-ns cljaws.core})
