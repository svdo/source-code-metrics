(ns metrics-server.sonar-fetcher
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.json :as json]))

(defn url [endpoint] (str "<redacted>" endpoint))
(def measures-component "/api/measures/component")
(def measures-component-tree "/api/measures/component_tree")

(defn raw-metric-page [project-id metric page page-size token]
  (client/get (url measures-component-tree)
              {:basic-auth (str token ":")
               :query-params {:component project-id
                              :metricKeys metric
                              :p page
                              :ps page-size
                              :strategy "leaves"}}))

(defn is-last-page [{:keys [pageIndex pageSize total]}]
  (> (* pageIndex pageSize) total))

(defn fetch-file-tree-metric
  ([metric config] (fetch-file-tree-metric metric config 1))
  ([metric config page]
   (let [project-id (:sonar-project-id config)
         page-size (:page-size config)
         token (:sonar-token config)
         response (raw-metric-page project-id metric page page-size token)
         measures (:body response)
         parsed (json/read-str measures :key-fn keyword)
         this-page (:components parsed)]
     (if (is-last-page (:paging parsed))
       this-page
       (reduce conj
               (fetch-file-tree-metric metric config (inc page))
               this-page)))))

(defn raw-metrics [project-id metrics token]
  (client/get (url measures-component)
              {:basic-auth (str token ":")
               :query-params {:component project-id
                              :metricKeys metrics}}))

(defn fetch-project-metrics [metrics config]
  (let [project-id (:sonar-project-id config)
        token (:sonar-token config)
        response (raw-metrics project-id (str/join "," metrics) token)
        measures (:body response)
        parsed (json/read-str measures :key-fn keyword)]
    (:component parsed)))

(comment
  (require '[clojure.string :as str])
  (def config (metrics-server.config/load-config))
  (client/get (url measures-component)
              {:basic-auth (str (:sonar-token config) ":")
               :query-params {:component (:sonar-project-id config)
                              :metricKeys "ncloc,new_coverage"}})
  (fetch-project-metrics ["ncloc" "new_coverage"] config))