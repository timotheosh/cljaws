(ns cljaws.dynamodb-test
  (:require [cljaws.dynamodb :as sut]
            [clojure.test :refer :all]))

(deftest test-placeholders
  (testing "Testing placeholder function"
    (is (= "foo_bar_baloo" (sut/->placeholder "foo-bar-baloo")))
    (is (= "foo_bar_baloo" (sut/->placeholder "foo/bar*baloo"))))
  (testing "Testing placeholder name"
    (is (= "#bing_go_mania" (sut/->placeholder-name "bing^go)mania")))
    (is (= "#bing_go_mania" (sut/->placeholder-name "bing:go:mania"))))
  (testing "Testing placeholder value"
    (is (= ":customer_first_name") (sut/->placeholder-value "customer:first-name"))
    (is (= ":customer_first_name") (sut/->placeholder-value "customer=first name"))))

(deftest test-map->typed
  (testing "Testing map->typed"
    (is (= {:M {"foo" {:S "bar"}, "baloo" {:S "bang"}, "num" {:N "34"}}} (sut/map->typed {:foo "bar" :baloo "bang" :num 34})))
    (is (= {:M
            {"entity-id" {:S "id35"},
             "entity-type" {:S "something"},
             "entity-data" {:L [{:M {"id" {:N "35"}, "name" {:S "foo"}}}]}}}
           (sut/map->typed {:entity-id "id35" :entity-type "something" :entity-data [{:id 35 :name "foo"}]})))))

(deftest test-typed->map
  (testing "Testing map->typed"
    (is (= {:foo "bar" :baloo "bang" :num 34}  (sut/typed->map {:M {"foo" {:S "bar"}, "baloo" {:S "bang"}, "num" {:N "34"}}})))
    (is (= {:entity-id "id35" :entity-type "something" :entity-data [{:id 35 :name "foo"}]}
           (sut/typed->map {:M
                            {"entity-id" {:S "id35"},
                             "entity-type" {:S "something"},
                             "entity-data" {:L [{:M {"id" {:N "35"}, "name" {:S "foo"}}}]}}})))))

(deftest test-format-put-item
  (testing "Entries with just a primary key"
    (is (= {:op :PutItem,
            :request {:TableName "jira-account-map", :Item {"id" {:S "item1"}}}}
           (sut/format-put-item "jira-account-map" {:id "item1"} {}))))
  (testing "Entries with a primary key and sort key"
    (is (= {:op :PutItem,
            :request
            {:TableName "some-table",
             :Item {"AccountId" {:S "accountid57"}, "Email" {:S "foo@bar.com"}}}}
           (sut/format-put-item "some-table" {:AccountId "accountid57"} {:Email "foo@bar.com"} {}))))
  (testing "Entries with a primary key and attributes"
    (is (= {:op :PutItem, :request {:TableName "another-table", :Item
                                    {"id" {:S "item2"},
                                     "description" {:S "Description for item 2"},
                                     "price" {:S "5.00"}}}}
           (sut/format-put-item "another-table" {:id "item2"} {:description "Description for item 2" :price "5.00"}))))
  (testing "Entries with a primary-key, sort-key, and attributes"
    (is (= {:op :PutItem,
            :request {:TableName "jira-resource-manager",
                      :Item {"entity-type" {:S "resource"},
                             "entity-id" {:S "AWS SSO Dev"},
                             "okta-group-ids" {:L [{:S "somGroupId"} {:S "anotherGroupId"}]},
                             "description" {:S "AWS SSO developers account"}}}}
           (sut/format-put-item "jira-resource-manager"
                                {:entity-type "resource"} {:entity-id "AWS SSO Dev"}
                                {:okta-group-ids ["somGroupId" "anotherGroupId"]
                                 :description "AWS SSO developers account"})))))

(deftest test-format-update-item
  (testing "PK, Change in attributes, no removals"
    (is (= {:op :UpdateItem,
            :request
            {:TableName "jira-resource-manager",
             :Key {"entity-type" {:S "group"}},
             :UpdateExpression "SET #resources = :resources",
             :ExpressionAttributeNames {"#resources" "resources"},
             :ExpressionAttributeValues
             {":resources"
              {:L [{:S "AWS SSO Dev"} {:S "AWS SSO Test"} {:S "Databricks"}]}}}}
           (sut/format-update-item "jira-resource-manager" {:entity-type "group"} nil
                                   {:resources ["AWS SSO Dev" "AWS SSO Test" "Databricks"]} []))))
  (testing "PK, Change in attributes, removals"
    (is (= {:op :UpdateItem,
            :request
            {:TableName "jira-resource-manager",
             :Key {"entity-type" {:S "group"}},
             :UpdateExpression "SET #resources = :resources REMOVE #no_op",
             :ExpressionAttributeNames {"#resources" "resources", "#no_op" "no-op"},
             :ExpressionAttributeValues
             {":resources"
              {:L [{:S "AWS SSO Dev"} {:S "AWS SSO Test"} {:S "Databricks"}]}}}}
           (sut/format-update-item "jira-resource-manager" {:entity-type "group"} nil
                                   {:resources ["AWS SSO Dev" "AWS SSO Test" "Databricks"]} ["no-op"]))))
  (testing "PK, SK, Change in attributes, no removals"
    (is (= {:op :UpdateItem,
            :request
            {:TableName "jira-resource-manager",
             :Key {"entity-type" {:S "group"}, "entity-id" {:S "Fulltime Dev"}},
             :UpdateExpression "SET #resources = :resources",
             :ExpressionAttributeNames {"#resources" "resources"},
             :ExpressionAttributeValues
             {":resources"
              {:L
               [{:S "AWS SSO Dev"}
                {:S "AWS SSO Test"}
                {:S "AWS SSO Prod"}
                {:S "PagerDuty"}
                {:S "Databricks"}]}}}}
           (sut/format-update-item "jira-resource-manager" {:entity-type "group"} {:entity-id "Fulltime Dev"}
                                   {:resources ["AWS SSO Dev" "AWS SSO Test" "AWS SSO Prod" "PagerDuty" "Databricks"]} []))))
  (testing "PK, SK, Change in attributes, removals"
    (is (= {:op :UpdateItem,
            :request
            {:TableName "jira-resource-manager",
             :Key {"entity-type" {:S "group"}, "entity-id" {:S "Intern"}},
             :UpdateExpression "SET #resources = :resources REMOVE #no_op",
             :ExpressionAttributeNames {"#resources" "resources", "#no_op" "no-op"},
             :ExpressionAttributeValues
             {":resources"
              {:L [{:S "AWS SSO Dev"} {:S "AWS SSO Test"} {:S "Databricks"}]}}}}
           (sut/format-update-item "jira-resource-manager" {:entity-type "group"} {:entity-id "Intern"}
                                   {:resources ["AWS SSO Dev" "AWS SSO Test" "Databricks"]} ["no-op"])))))

(deftest test-format-batch-put
  (testing "Test format-batch-put"
    (is (= {:PutRequest
            {:Item
             {"AccountId" {:S "key1"},
              "Email" {:S "john.doe@missing.persons"},
              "description" {:S "Something"},
              "resources" {:L [{:S "resource1"} {:S "resource2"}]}}}}
           (sut/format-batch-put {:pk {:AccountId "key1"}
                                  :sk {:Email "john.doe@missing.persons"}
                                  :attributes {:description "Something"
                                               :resources ["resource1" "resource2"]}})))))

(deftest test-format-batch-delete
  (testing "Test format-batch-delete"
    (is (= {:DeleteRequest {:Key {"AccountId" {:S "jfdjd"}, "Email" {:S "foo@bar.com"}}}}
           (sut/format-batch-delete {:pk {:AccountId "jfdjd"}
                                     :sk {:Email "foo@bar.com"}})))))

(deftest test-format-batch-operations
  (testing "Test format-batch-operations"
    (is (= [{:PutRequest
             {:Item
              {"description" {:S "Something"},
               "resources" {:L [{:S "resource1"} {:S "resource2"}]},
               "AccountId" {:S "key1"},
               "Email" {:S "john.doe@missing.persons"}}}}
            {:DeleteRequest {:Key {"AccountId" {:S "jfdjd"}, "Email" {:S "foo@bar.com"}}}}]
           (sut/format-batch-operations {:put [{:pk {:AccountId "key1"}
                                                :sk {:Email "john.doe@missing.persons"}
                                                :attributes {:description "Something"
                                                             :resources ["resource1" "resource2"]}}]
                                         :delete [{:pk {:AccountId "jfdjd"}
                                                   :sk {:Email "foo@bar.com"}}]})))))

(deftest test-format-batch-write
  (testing "Multiple tables with put and delete items."
    (is (= {:op :BatchWriteItem,
            :request
            {:RequestItems
             {:jira-account-map
              [{:PutRequest
                {:Item
                 {"AccountId" {:S "key1"}, "Email" {:S "john.doe@missing.persons"}}}}
               {:DeleteRequest
                {:Key {"AccountId" {:S "jfdjd"}, "Email" {:S "foo@bar.com"}}}}],
              :jira-resource-manager
              [{:PutRequest
                {:Item
                 {"entity-type" {:S "resource"},
                  "entity-id" {:S "New Relic"},
                  "okta-group-ids" {:L [{:S "yetAnotherGroupId"}]}}}}
               {:PutRequest
                {:Item {"entity-type" {:S "resource"}, "entity-id" {:S "Odin"}}}}
               {:DeleteRequest
                {:Key {"entity-type" {:S "resource"}, "entity-id" {:S "Databricks"}}}}]}}}
           (sut/format-batch-write {:jira-account-map
                                    {:put [{:pk {:AccountId "key1"}
                                            :sk {:Email "john.doe@missing.persons"}
                                            :description "Something"
                                            :resources ["resource1" "resource2"]}]
                                     :delete [{:pk {:AccountId "jfdjd"}
                                               :sk {:Email "foo@bar.com"}}]}
                                    :jira-resource-manager
                                    {:put [{:pk {:entity-type "resource"} :sk {:entity-id "New Relic"}
                                            :attributes {:okta-group-ids ["yetAnotherGroupId"]}}
                                           {:pk {:entity-type "resource"} :sk {:entity-id "Odin"}
                                            :attibutes {:okta-group-ids ["andAnotherGroupId"]}}]
                                     :delete [{:pk {:entity-type "resource"} :sk {:entity-id "Databricks"}}]}})))))
