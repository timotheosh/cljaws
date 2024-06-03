(ns cljaws.dynamodb
  (:require [cognitect.aws.client.api :as aws]
            [cljaws.aws-client :as aws-client]
            [cljaws.config :refer [get-env get-region]]))

(defn ->placeholder [attr]
  (clojure.string/replace (name attr) #"[^a-zA-Z0-9_]" "_"))

(defn ->placeholder-name [attr]
  (str "#" (->placeholder attr)))

(defn ->placeholder-value [attr]
  (str ":" (->placeholder attr)))

(defn format-value [value]
  (cond
    (string? value) {:S value}  ; String type
    (number? value) {:N (str value)}  ; Number type, converted to string as DynamoDB expects numbers as strings
    (boolean? value) {:BOOL value}  ; Boolean type
    (nil? value) {:NULL true}  ; Null type
    (map? value) {:M (into {} (map (fn [[k v]] [(name k) (format-value v)]) value))}  ; Map type
    (set? value) {:SS (mapv str value)}  ; String set type (all elements must be strings)
    (vector? value) {:L (mapv format-value value)}  ; List type, recursive for nested structures
    :else (throw (ex-info "Unsupported attribute type" {:value value}))))

(defn scan-table
  "Returns a list of all items in a DynamoDB table"
  ([table-name] (scan-table table-name :default (get-region :default)))
  ([table-name environment] (scan-table table-name environment (get-region environment)))
  ([table-name environment region]
   (aws-client/awscli
    :dynamodb
    {:op :Scan
     :request {:TableName table-name}}
    environment region)))

(defn format-put-item
  "Formats data for puting an item into the DynamoDB table."
  [table-name entity-type entity-id attributes]
  {:op      :PutItem
   :request {:TableName table-name
             :Item      (into {'entity-type {:S entity-type}
                               'entity-id   {:S entity-id}}
                              (mapv (fn [[key value]] [key (format-value value)]) attributes))}})

(defn format-update-item
  "Formats data for updating an item in the DynamoDB table, allowing attribute additions, updates, and deletions."
  ([table-name entity-type entity-id updates] (format-update-item table-name entity-type entity-id updates nil))
  ([table-name entity-type entity-id updates removals]
   (let [update-sets (when (and updates (not (empty? updates)))
                       (str "SET " (clojure.string/join ", " (map (fn [[key _]] (str (->placeholder-name key) " = "
                                                                                     (->placeholder-value key))) updates))))
         remove-sets (when (and removals (not (empty? removals)))
                       (str "REMOVE " (clojure.string/join ", " (map ->placeholder-name removals))))
         update-expr (clojure.string/trim (str update-sets (when (and update-sets remove-sets) " ") remove-sets))
         expr-attr-nams (into {} (concat (when updates (map (fn [[key _]] {(->placeholder-name key) (name key)}) updates))
                                         (when removals (map (fn [key] {(->placeholder-name key) (name key)}) removals))))
         expr-attr-vals (when updates (into {} (map (fn [[key value]] {(->placeholder-value key) (format-value value)}) updates)))]
     {:op :UpdateItem
      :request (cond-> {:TableName table-name
                        :Key       {:entity-type {:S entity-type}
                                    :entity-id   {:S entity-id}}
                        :UpdateExpression update-expr
                        :ExpressionAttributeNames expr-attr-nams}
                 expr-attr-vals (assoc :ExpressionAttributeValues expr-attr-vals))})))

(defn format-delete-item
  "Formats data for deleting an item from the DynamoDB table."
  [table-name entity-type entity-id]
  {:op :DeleteItem
   :request {:TableName table-name
             :Key {:entity-type {:S entity-type}
                   :entity-id {:S entity-id}}}})

(defn put-item
  "Adds an item to the dynamodb table."
  [entity-type entity-id resources]
  (format-put-item db-table-name entity-type entity-id {:resources resources}))

(defn update-item
  ([entity-type entity-id updates] (update-item entity-type entity-id updates nil))
  ([entity-type entity-id updates removals]
   (format-update-item db-table-name entity-type entity-id updates removals)))
