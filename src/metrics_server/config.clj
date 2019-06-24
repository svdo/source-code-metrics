(ns metrics-server.config
  (:require [metrics-server.load-edn :refer (load-edn load-edn!)]
            [clojure.spec.alpha :as s]
            [expound.alpha :as e])
  (:import org.apache.commons.validator.routines.UrlValidator))

(def valid-url? #(.isValid (UrlValidator.) %))

(s/def :sonar/base-url valid-url?)
(s/def :sonar/token (s/and string? (complement empty?)))
(s/def :sonar/project-id (s/and string? (complement empty?)))

(s/def :gitlab/base-url valid-url?)
(s/def :gitlab/token (s/and string? (complement empty?)))
(s/def :gitlab/project-id pos-int?)

(s/def :report/project
  (s/keys :req [:sonar/token
                :sonar/project-id
                :gitlab/token
                :gitlab/project-id]))

(s/def :report/projects
  (s/+ :report/project))

(s/def ::config
  (s/keys :req [:sonar/base-url
                :gitlab/base-url
                :fetch/page-size
                :complexity/orange-threshold
                :complexity/red-threshold
                :report/projects]))

(defn load-config []
  (let [config       (load-edn "config.edn")
        local-config (load-edn! "config.local.edn")
        merged       (merge config local-config)]
    (if (s/valid? ::config merged)
      merged
      (throw (ex-info (e/expound-str ::config merged)
                      (s/explain-data ::config merged))))))

(comment
  (def config (metrics-server.config/load-config))
  (e/expound ::config config)
  (let [config       (load-edn "config.edn")
        local-config (load-edn! "config.local.edn")
        merged       (merge config local-config)]
    (e/expound ::config merged)))