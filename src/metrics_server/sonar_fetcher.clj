(ns metrics-server.sonar-fetcher
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(defn url [endpoint] (str "<redacted>" endpoint))
(def measures-component-tree "/api/measures/component_tree")

(defn raw-metric-page [project-id metric page page-size token]
  (client/get (url measures-component-tree)
              {:basic-auth (str token ":")
               :query-params {:component project-id
                              :metricKeys metric
                              :p page
                              :ps page-size}}))

(defn is-last-page [{:keys [pageIndex pageSize total]}]
  (> (* pageIndex pageSize) total))

(defn fetch-metric
  ([metric config] (fetch-metric metric config 1))
  ([metric config page]
   (let [project-id (:project-id config)
         page-size (:page-size config)
         token (:token config)
         response (raw-metric-page project-id metric page page-size token)
         measures (:body response)
         parsed (json/read-str measures :key-fn keyword)
         this-page (:components parsed)]
     (if (is-last-page (:paging parsed))
       this-page
       (reduce conj
               (fetch-metric metric config (inc page))
               this-page)))))


(comment
  (def measures-component "/api/measures/component")
  (def config (metrics-server.config/load-config))
  (client/get (url measures-component)
              {:basic-auth (str (:token config) ":")
               :query-params {:component (:project-id config)
                              :metricKeys "ncloc,new_coverage"}}))