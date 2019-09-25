(ns cljaws.config
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn]))

(defn load-edn
  "Loads edn file."
  ([] (load-edn "/home/thawes/SpiderOak Hive/Work_Laptop/data/aws-accounts.edn"))
  ([edn-path]
   (try
     (with-open [r (io/reader edn-path)]
       (edn/read (java.io.PushbackReader. r)))
     (catch java.io.IOException e
       (printf "Couldn't open '%s': %s\n" edn-path (.getMessage e)))
     (catch RuntimeException e
       (printf "Error parsing edn file '%s': %s\n" edn-path (.getMessage e))))))

(def config (load-edn))

(defn get-env
  [env]
  (if (str/starts-with? (name env) "prod")
    :prod
    env))

(defn accountid
  "Returns the accountid of the given environment."
  [env]
  (:accountid ((get-env env) config)))

(defn get-region
  "Returns the region for a given environment"
  [env]
  (let [info (mapv keyword (str/split (name env) #"-"))]
    (if (> (count info) 1)
      ((last info) (:regions ((first info) config)))
      (:default (:regions ((first info) config))))))
