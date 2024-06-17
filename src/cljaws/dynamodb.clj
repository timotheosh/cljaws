(ns cljaws.dynamodb
  (:require [clojure.spec.alpha :as s]
            [cognitect.aws.client.api :as aws]
            [cljaws.aws-client :refer [create-client awscli async *client* error?]]
            [cljaws.config :refer [get-env get-region]]))

(def ^{:dynamic true :private false} *table-name* nil)

(s/def :dynamodb/table-name string?)

(s/def :dynamodb/dynamodb-key (fn [key] (= key :pk) (= key :sk)))
(s/def :dynamodb/dynamodb-value (s/or :string string?
                                      :number number?
                                      :bytes bytes?))

(s/def :dynamodb/attributes map?)

(defn- validate-table-name [table-name]
  (if (s/valid? :dynamodb/table-name table-name)
    table-name
    (throw (ex-info "Invalid table-name" {:table-name table-name}))))

(defn- validate-key [key]
  (when-not (s/valid? :dynamodb/dynamodb-value key)
    (throw (ex-info (str "Invalid " key ": is of type " (type key)
                         " Must be either a string, number, or binary") {:key key}))))

(defn- validate-key-pair [pair]
  (when-not (map? pair) (ex-info (str "Invalid " pair ": is of type " (type pair)
                                      " Must be a map.") {:pair pair}))
  (let [key (first (keys pair))
        value (first (val pair))]
    (when-not (s/valid? :dynamodb/dynamodb-key value)
      (throw (ex-info (str "Invalid " value ": is of type " (type value)
                           " Must be either a string, number, or binary") {:pair pair})))))

(defn- validate-attributes [attr]
  (if (s/valid? :dynamodb/attributes attr)
    attr
    (throw (ex-info "Attributes must be sent as a map " {:attributes attr}))))

(defn ->placeholder
  "Converts key and attribute names and values within an UpdateItem call."
  [attr]
  (clojure.string/replace (name attr) #"[^a-zA-Z0-9_]" "_"))

(defn ->placeholder-name [attr]
  (str "#" (->placeholder attr)))

(defn ->placeholder-value [attr]
  (str ":" (->placeholder attr)))

(defn map->typed
  "Takes a value and assigns the appropriate data type for dynamodb"
  [value]
  (try
    (cond
      (string? value) {:S value}
      (number? value) {:N (str value)}
      (bytes? value) {:B value}
      (boolean? value) {:BOOL value}
      (nil? value) {:NULL true}
      (map? value) {:M (into {} (map (fn [[k v]] [(name k) (map->typed v)]) value))}
      (set? value) {:SS (mapv str value)}
      (vector? value) {:L (mapv map->typed value)}
      :else (throw (ex-info "Unsupported attribute type" {:value value})))
    (catch Exception err (ex-data err))))

(defn typed->map
  "Takes a DynamoDB typed value and converts it to the appropriate Clojure type"
  [typed-value]
  (try
    (cond
      (map? typed-value)
      (cond
        (:S typed-value) (:S typed-value)
        (:N typed-value) (read-string (:N typed-value))
        (:B typed-value) (:B typed-value)
        (:BOOL typed-value) (:BOOL typed-value)
        (:NULL typed-value) nil
        (:M typed-value) (into {} (map (fn [[k v]] [(keyword k) (typed->map v)]) (:M typed-value)))
        (:L typed-value) (mapv typed->map (:L typed-value))
        (:SS typed-value) (set (:SS typed-value))
        (:NS typed-value) (set (map read-string (:NS typed-value)))
        (:BS typed-value) (set (:BS typed-value))
        :else (into {} (map (fn [[k v]] [(keyword k) (typed->map v)]) typed-value)))

      ;; Directly handle collections
      (vector? typed-value) (mapv typed->map typed-value)
      (set? typed-value) (set (map typed->map typed-value))

      ;; Handle direct values
      (string? typed-value) typed-value
      (number? typed-value) typed-value
      (boolean? typed-value) typed-value
      (nil? typed-value) nil

      :else (throw (ex-info "Unsupported attribute type" {:typed-value typed-value})))
    (catch Exception err (ex-data err))))

(defn format-put-item
  "Formats data for putting an item into the DynamoDB table. Supports optional sort key."
  ([table-name pk attributes]
   (validate-table-name table-name)
   (doseq [[k v] pk]
     (validate-key v))
   {:op      :PutItem
    :request {:TableName table-name
              :Item      (into (into {} (map (fn [[k v]] [(name k) (map->typed v)]) pk))
                               (mapv (fn [[key value]] [(name key) (map->typed value)]) attributes))}})
  ([table-name pk sk attributes]
   (validate-table-name table-name)
   (doseq [[k v] (merge pk sk)]
     (validate-key v))
   {:op      :PutItem
    :request {:TableName table-name
              :Item      (into (into {} (map (fn [[k v]] [(name k) (map->typed v)]) (merge pk sk)))
                               (mapv (fn [[key value]] [(name key) (map->typed value)]) attributes))}}))

(defn format-update-item
  "Formats data for updating an item in the DynamoDB table, allowing attribute additions, updates, and deletions."
  ([table-name pk updates]
   (format-update-item table-name pk nil updates nil))
  ([table-name pk sk updates removals]
   (doseq [[k v] (merge pk sk)]
     (validate-key v))
   (validate-table-name table-name)
   (let [update-sets (when (and updates (not (empty? updates)))
                       (str "SET " (clojure.string/join ", " (map (fn [[key _]] (str (->placeholder-name key) " = "
                                                                                     (->placeholder-value key))) updates))))
         remove-sets (when (and removals (not (empty? removals)))
                       (str "REMOVE " (clojure.string/join ", " (map ->placeholder-name removals))))
         update-expr (clojure.string/trim (str update-sets (when (and update-sets remove-sets) " ") remove-sets))
         expr-attr-nams (into {} (concat (when updates (map (fn [[key _]] {(->placeholder-name key) (name key)}) updates))
                                         (when removals (map (fn [key] {(->placeholder-name key) (name key)}) removals))))
         expr-attr-vals (when updates (into {} (map (fn [[key value]] {(->placeholder-value key) (map->typed value)}) updates)))]
     {:op :UpdateItem
      :request (cond-> {:TableName table-name
                        :Key       (into {} (concat (map (fn [[k v]] [(name k) (map->typed v)]) pk)
                                                    (map (fn [[k v]] [(name k) (map->typed v)]) (or sk {}))))
                        :UpdateExpression update-expr
                        :ExpressionAttributeNames expr-attr-nams}
                 expr-attr-vals (assoc :ExpressionAttributeValues expr-attr-vals))})))

(defn format-batch-put
  "Formats a put item for DynamoDB."
  [item]
  (let [keys (merge (:pk item) (:sk item))
        formatted-item (into {}
                             (concat
                              (map (fn [[k v]] [(name k) (map->typed v)]) keys)
                              (map (fn [[k v]] [(name k) (map->typed v)]) (:attributes item))))]
    {:PutRequest {:Item formatted-item}}))

(defn format-batch-delete
  "Formats a delete item for DynamoDB."
  [item]
  (let [key-map (into {}
                      (map (fn [[k v]] [(name k) (map->typed v)])
                           (merge (:pk item) (:sk item))))]
    (doseq [[k v] (merge (:pk item) (:sk item))]
      (validate-key v))
    {:DeleteRequest {:Key key-map}}))

(defn format-batch-operations
  "Formats the operations (put and delete) for DynamoDB."
  [ops]
  (let [put-requests (mapv format-batch-put (get ops :put []))
        delete-requests (mapv format-batch-delete (get ops :delete []))]
    (apply conj put-requests delete-requests)))

(defn format-batch-write
  "Formats data for a batch write operation in DynamoDB, supporting both put and delete operations, grouped by table."
  [requests]
  {:op :BatchWriteItem
   :request {:RequestItems (into {}
                                 (mapv (fn [[table-name ops]]
                                         [table-name (format-batch-operations ops)])
                                       requests))}})

(defn scan-table
  "Returns a list of all items in a DynamoDB table"
  ([table-name] (scan-table table-name :default (get-region :default)))
  ([table-name environment] (scan-table table-name environment (get-region environment)))
  ([table-name environment region]
   (validate-table-name table-name)
   (try
     (let [result (awscli
                   :dynamodb
                   {:op :Scan
                    :request {:TableName table-name}}
                   environment region)]
       (when (error? result)
         (throw (ex-info "Scan Error!" {:message result})))
       (typed->map result)))))

(defn put-item
  "Adds an item to the dynamodb table."
  ([entity-type entity-id resources] (put-item *table-name* entity-type entity-id resources))
  ([table-name entity-type entity-id resources]
   (format-put-item table-name entity-type entity-id resources)))

(defn update-item
  ([entity-type entity-id updates removals] (update-item *table-name* entity-type entity-id updates nil))
  ([table-name entity-type entity-id updates removals]
   (format-update-item table-name entity-type entity-id updates removals)))

(comment
  ;; Sample input data for a batch-write operation.
  (def r
    {:jira-account-map
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
      :delete [{:pk {:entity-type "resource"} :sk {:entity-id "Databricks"}}]}}
    )

  (def q
    {:op :BatchWriteItem,
     :request
     {:RequestItems
      {"jira-account-map"
       [{:PutRequest
         {:Item
          {"description" {:S "Something"},
           "resources" {:L [{:S "resource1"} {:S "resource2"}]},
           "AccountId" {:S "key1"},
           "Email" {:S "john.doe@missing.persons"}}}}
        {:DeleteRequest {:Key {"AccountId" {:S "jfdjd"}, "Email" {:S "foo@bar.com"}}}}],
       "jira-resource-manager"
       [{:PutRequest
         {:Item
          {"entity-type" {:S "resource"},
           "entity-id" {:S "New Relic"}
           "okta-group-ids" {:L [{:S "yetAnotherGroupId"}]}}}}
        {:PutRequest
         {:Item
          {"entity-type" {:S "resource"},
           "entity-id" {:S "Odin"}
           "okta-group-ids" {:L [{:S "andAnotherGroupId"}]}}}}
        {:DeleteRequest {:Key {"entity-type" {:S "resource"} "entity-id" {:S "Databricks"}}}}]}}})

  )
