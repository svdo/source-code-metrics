(ns metrics-server.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [metrics-server.config :refer (load-config)]))

(defn basic-auth [token] {:basic-auth (str token ":")})
(defn url [endpoint] (str "<redacted>" endpoint))
(def measures-component-tree "/api/measures/component_tree")

(defn categorize-complexity
  [entry {:keys [complexity-orange-threshold complexity-red-threshold]}]
  (let [value (:value entry)]
    (cond
      (< value complexity-orange-threshold) :green
      (< value complexity-red-threshold)    :orange
      :else                                 :red)))

(defn raw-metric-page [project-id metric page page-size token]
  (client/get (url measures-component-tree)
              (merge (basic-auth token)
                     {:query-params {:component project-id
                                     :metricKeys metric
                                     :p page
                                     :ps page-size}})))

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

(defn extract-relevant-fields [{:keys [key name]
                                [{:keys [value]}] :measures}]
  {:key key
   :name name
   :value value})

(defn value-string-to-number [entry]
  (update-in entry [:value] read-string))

(defn has-a-value [entry]
  (some? (:value entry)))

(defn is-a-file [entry]
  (not (str/includes? (:name entry) "/")))

(defn compute-categorized-complexity [metrics-data config]
  (let [interesting-part (map extract-relevant-fields metrics-data)]
    (->> interesting-part
         (filter has-a-value)
         (filter is-a-file)
         (map value-string-to-number)
         (map #(assoc % :category (categorize-complexity % config)))
         (sort-by :value)
         (reverse)
         (group-by :category))))

(defn -main []
  (let [config (load-config)
        metrics-data (fetch-metric "complexity" config)
        complexity (compute-categorized-complexity metrics-data config)]
    (println "Project key:" (:project-id config))
    (println "Complexity:")
    (println "  green:" (count (:green complexity)))
    (println "  orange:" (count (:orange complexity)))
    (println "  red:" (count (:red complexity)))))
