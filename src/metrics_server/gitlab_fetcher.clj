(ns metrics-server.gitlab-fetcher
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.json :as json])
  (:import java.time.ZoneId
           java.time.format.DateTimeFormatter
           java.time.temporal.TemporalAdjusters
           java.time.LocalDate
           java.time.DayOfWeek))

(def zone-id (. ZoneId systemDefault))
(def formatter (. DateTimeFormatter ISO_OFFSET_DATE_TIME))
(defn date-to-string [date-time]
  (-> date-time
      (. atStartOfDay)
      (. atZone zone-id)
      (. format formatter)))
(def last-monday
  (-> (. LocalDate now)
      (. with (. TemporalAdjusters previousOrSame DayOfWeek/MONDAY))))
(def before-last-monday
  (-> last-monday
      (. with (. TemporalAdjusters previous DayOfWeek/MONDAY))))

(defn url [endpoint] (str "<redacted>" endpoint))
(defn commits [project-id]
  (format "/projects/%s/repository/commits" project-id))

(defn raw-metrics [project-id from to token]
  (let [commit-url (url (commits project-id))]
    (client/get commit-url
                {:query-params {:private_token token
                                :since (date-to-string from)
                                :until (date-to-string to)
                                :with_stats true}})))

(defn fetch-commit-details
  ([config]
   (let [from before-last-monday
         to last-monday]
     (fetch-commit-details from to config)))
  ([from to config]
   (let [project-id (:gitlab-project-id config)
         token (:gitlab-token config)
         response (raw-metrics project-id from to token)
         measures (:body response)
         parsed (json/read-str measures :key-fn keyword)]
     parsed)))

(comment
  (def config (metrics-server.config/load-config))

  (def before-before-last-monday
    (-> before-last-monday
        (. with (. TemporalAdjusters previous DayOfWeek/MONDAY))))

  (fetch-commit-details config)
  (fetch-commit-details before-before-last-monday
                        before-last-monday
                        config))