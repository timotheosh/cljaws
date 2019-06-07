(ns cljaws.datetime
  (:import [java.text SimpleDateFormat]
           [java.util
            TimeZone
            Date]
           [java.time
            LocalDateTime
            ZonedDateTime
            ZoneId]))

;; Not true iso format, but close enough.
(def iso-format (new SimpleDateFormat "yyyy-MM-dd HH:mm:ss+00:00"))
;; Simple 2019-04-16 date format.
(def y-m-d-format (new SimpleDateFormat "yyyy-MM-dd"))

(.setTimeZone iso-format (TimeZone/getTimeZone "UTC"))


(defn date->java
  "Convert text date to java.util.Date"
  [date-text]
  (when-not (nil? date-text)
    (.parse iso-format date-text)))

(defn java->date
  "Convert java.util.Date to text."
  [date]
  (when-not (nil? date)
    (.format iso-format date)))

(defn date->zoneddate
  "Convert java.util.Date to java.time.ZonedDateTime"
  [date]
  (ZonedDateTime/ofInstant (.toInstant date) (ZoneId/systemDefault)))

(defn zoneddate->date
  "Convert java.time.ZonedDateTime to java.util.Date"
  [zoned]
  (Date/from (.toInstant zoned)))

(defn days-ago
  "Returns date string from days ago given."
  [minus-days]
  (.format y-m-d-format (zoneddate->date (.minusDays (ZonedDateTime/now) minus-days))))

(defn after?
  "Returns true if given java.util.Date falls after the given number of days after now."
  [date days]
  (.isAfter (date->zoneddate date)
            (.minusDays (ZonedDateTime/now) days)))

(defn before?
  "Returns true if given java.util.Date falls after the given number of days before now."
  [date days]
  (.isBefore (date->zoneddate date)
             (.minusDays (ZonedDateTime/now) days)))

(defn epoch
  "Takes java.util.Date and converts to epoch."
  [date]
  (when date
    (.getTime date)))
