(defproject timotheosh/cljaws "0.3.8-SNAPSHOT"
  :description "Convenience functions for interacting with AWS API's from closh"
  :url "https://github.com/timotheosh/cljaws"
  :license {:name "MIT"
            :url "https://www.opensource.org/licenses/MIT"}
  :plugins [[lein-tools-deps "0.4.5"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}
  :repl-options {:init-ns cljaws.core})
