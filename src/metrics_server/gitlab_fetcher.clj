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

(def formatter (DateTimeFormatter/ISO_OFFSET_DATE_TIME))

(defn date->string [^LocalDate date-time]
  (.. date-time
      (atStartOfDay)
      (atZone ^ZoneId (ZoneId/systemDefault))
      (format formatter)))

(defn ^Temporal last-monday []
  (.. LocalDate
      (now)
      (with (TemporalAdjusters/previousOrSame DayOfWeek/MONDAY))))

(defn before-last-monday []
  (.. (last-monday)
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
  [project-id from to token commits-url page-getter]
  (let [response (page-getter from to token commits-url)
        headers  (:headers response)
        links    (parse-link-header (headers "Link"))
        measures (:body response)
        parsed   (json/read-str measures :key-fn keyword)]
    (if-let [next (:next links)]
      (concat parsed (parse-measures project-id from to token next page-getter))
      parsed)))

(defn- get-page [from to token page-url]
  (client/get page-url
              {:query-params {:private_token token
                              :since         (date->string from)
                              :until         (date->string to)
                              :with_stats    true
                              :per_page      1}}))

(defn fetch-commit-details
  ([config]
   (let [from (before-last-monday)
         to   (last-monday)]
     (fetch-commit-details from to config)))
  ([from to config]
   (let [project-id (:gitlab/project-id config)
         token      (:gitlab/token config)]
     (parse-measures project-id from to token
                     (url config (commits project-id))
                     get-page))))

(comment
  (require '[metrics-server.config :refer (load-config)])
  (def config (load-config))
  (def first-project (first (:report/projects config)))
  (def first-project-config (merge (dissoc config :report/projects) first-project))

  (defn before-before-last-monday []
    (.. (before-last-monday)
        (with (TemporalAdjusters/previous DayOfWeek/MONDAY))))

  (let [project-id (:gitlab/project-id first-project-config)]
    (parse-measures project-id
                    (before-last-monday)
                    (last-monday)
                    (:gitlab/token first-project-config)
                    (url first-project-config (commits project-id))))

  (fetch-commit-details first-project-config)
  (fetch-commit-details (before-before-last-monday)
                        (before-last-monday)
                        first-project-config))
