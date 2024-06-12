(ns cljaws.dynamo-spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(require '[clojure.spec.alpha :as s])

;; Define the acceptable value types
(s/def ::value (s/or :string string?
                     :number number?
                     :bytes bytes?))

;; Define the spec for the DynamoDB key map
(s/def ::dynamodb-key
  (s/and (s/map-of (s/and keyword? #{:pk :sk}) ::value :count 1)))

;; Examples of validating maps
(s/valid? ::dynamodb-key {:pk "some string"})    ;; true
(s/valid? ::dynamodb-key {:sk 123})              ;; true
(s/valid? ::dynamodb-key {:pk (byte-array 10)})  ;; true
(s/valid? ::dynamodb-key {:pk "some string" :sk 123}) ;; false, only one key allowed
(s/valid? ::dynamodb-key {:pk [1 2 3]})          ;; false, value is not string, number, or bytes
