(ns metrics-server.sonar-fetcher
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.json :as json]))

(defn- url [config endpoint] (str (:sonar/base-url config) endpoint))
(def ^:private measures-component "/api/measures/component")
(def ^:private measures-component-tree "/api/measures/component_tree")

(defn- raw-metric-page
  [measures-component-tree-url project-id metric page page-size token]
  (client/get measures-component-tree-url
              {:basic-auth   (str token ":")
               :query-params {:component  project-id
                              :metricKeys metric
                              :p          page
                              :ps         page-size
                              :strategy   "leaves"}}))

(defn- is-last-page [{:keys [pageIndex pageSize total]}]
  (>= (* pageIndex pageSize) total))

(defn fetch-file-tree-metric
  ([metric config] (fetch-file-tree-metric metric config 1 raw-metric-page))
  ([metric config page page-getter]
   (let [project-id (:sonar/project-id config)
         page-size  (:fetch/page-size config)
         token      (:sonar/token config)
         response   (page-getter (url config measures-component-tree)
                                 project-id metric page page-size token)
         measures   (:body response)
         parsed     (json/read-str measures :key-fn keyword)
         this-page  (:components parsed)]
     (if (is-last-page (:paging parsed))
       this-page
       (reduce conj
               this-page
               (fetch-file-tree-metric metric config (inc page) page-getter))))))


(defn- raw-metrics [measures-component-url project-id metrics token]
  (client/get measures-component-url
              {:basic-auth   (str token ":")
               :query-params {:component  project-id
                              :metricKeys metrics}}))

(defn fetch-project-metrics
  ([metrics config]
   (fetch-project-metrics metrics config raw-metrics))
  ([metrics config data-getter]
   (let [project-id (:sonar/project-id config)
         token      (:sonar/token config)
         response   (data-getter (url config measures-component)
                                 project-id (str/join "," metrics) token)
         measures   (:body response)
         parsed     (json/read-str measures :key-fn keyword)]
     (:component parsed))))

(comment
  (require '[metrics-server.config :refer (load-config)])
  (def config (load-config))
  (def first-project (first (:report/projects config)))
  (def first-project-config (merge (dissoc config :report/projects) first-project))
  (client/get (url config measures-component)
              {:basic-auth   (str (:sonar/token first-project) ":")
               :query-params {:component  (:sonar/project-id first-project)
                              :metricKeys "ncloc,new_coverage,vulnerabilities"}})
  (json/read-str (:body *1) :key-fn keyword)
  (fetch-project-metrics ["ncloc" "new_coverage" "vulnerabilities"] first-project-config)
  (fetch-file-tree-metric "complexity" first-project-config))
