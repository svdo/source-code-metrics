(ns metrics-server.gitlab-fetcher
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.json :as json])
  (:import java.time.ZoneId
           java.time.format.DateTimeFormatter
           java.time.temporal.TemporalAdjusters
           java.time.LocalDate
           java.time.DayOfWeek
           java.time.temporal.Temporal))

(def ^ZoneId zone-id (ZoneId/systemDefault))
(def formatter (DateTimeFormatter/ISO_OFFSET_DATE_TIME))

(defn date-to-string [^LocalDate date-time]
  (.. date-time
      (atStartOfDay)
      (atZone zone-id)
      (format formatter)))

(def ^Temporal last-monday
  (.. LocalDate
      (now)
      (with (TemporalAdjusters/previousOrSame DayOfWeek/MONDAY))))

(def before-last-monday
  (.. last-monday
      (with (TemporalAdjusters/previous DayOfWeek/MONDAY))))

(defn- url [config endpoint] (str (:gitlab/base-url config) endpoint))
(defn- commits [project-id]
  (format "/projects/%s/repository/commits" project-id))

(defn- parse-link [link]
  (let [[_ url key] (re-matches #".*<(.*)>;.*rel=\"(.*)\".*" link)]
    {(keyword key) url}))

(defn- parse-link-header [link-header]
  (let [links (clojure.string/split link-header #", ")]
    (reduce merge {} (mapv parse-link links))))

(defn- parse-measures
  [project-id from to token commits-url]
  (let [response (client/get commits-url
                             {:query-params {:private_token token
                                             :since         (date-to-string from)
                                             :until         (date-to-string to)
                                             :with_stats    true
                                             :per_page      100}})
        headers  (:headers response)
        links    (parse-link-header (headers "link"))
        measures (:body response)
        parsed   (json/read-str measures :key-fn keyword)]
    (if-let [next (:next links)]
      (concat parsed (parse-measures project-id from to token next))
      parsed)))

(defn fetch-commit-details
  ([config]
   (let [from before-last-monday
         to   last-monday]
     (fetch-commit-details from to config)))
  ([from to config]
   (let [project-id (:gitlab/project-id config)
         token      (:gitlab/token config)]
     (parse-measures project-id from to token
                     (url config (commits project-id))))))

(comment
  (def config (metrics-server.config/load-config))

  (def before-before-last-monday
    (.. before-last-monday
        (with (TemporalAdjusters/previous DayOfWeek/MONDAY))))

  (let [project-id (:gitlab/project-id config)]
    (parse-measures project-id
                    before-last-monday
                    last-monday
                    (:gitlab/token config)
                    (url config (commits project-id))))

  (fetch-commit-details config)
  (fetch-commit-details before-before-last-monday
                        before-last-monday
                        config))