(ns cljaws.service-quotas
  (:require [clojure.string :as str]
            [cognitect.aws.client.api :as aws]
            [cljaws.aws-client :as aws-client]
            [cljaws.config :refer [accountid get-env get-region]]))
