(ns cljaws.aws-client-test
  (:require [cljaws.aws-client :as sut]
            [clojure.test :refer :all]))

(deftest test-error-checking
  (testing "Can we catch errors in AWS responses?"
    (is (nil? (sut/error? {:Response {:Errors []}})))
    (is (= {:Response {:Errors ["Error message"]}} (sut/error? {:Response {:Errors ["Error message"]}})))
    (is (nil? (sut/error? {:Error nil})))
    (is (= {:Error "message"} (sut/error? {:Error "message"})))))
