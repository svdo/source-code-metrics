(ns metrics-server.core
  (:gen-class)
  (:require [clj-http.client :as client]
            ; [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [metrics-server.config :refer (load-config)]))

(def config (atom nil))
(def auth-token (atom nil))

(defn basic-auth [] {:basic-auth (str @auth-token ":")})
(defn url [endpoint] (str "<redacted>" endpoint))
(def measures-component-tree "/api/measures/component_tree")

(defn categorize-complexity [orange-threshold red-threshold entry]
  (let [value (:value entry)]
    (cond
      (< value orange-threshold) :green
      (< value red-threshold)    :orange
      :else                      :red)))

(defn raw-metric-page [project-id metric page page-size]
  (client/get (url measures-component-tree)
              (merge (basic-auth)
                     {:query-params {:component project-id
                                     :metricKeys metric
                                     :p page
                                     :ps page-size}})))

(defn is-last-page [{:keys [pageIndex pageSize total]}]
  (> (* pageIndex pageSize) total))

(defn fetch-metric
  ([project-id metric page-size] (fetch-metric project-id metric page-size 1))
  ([project-id metric page-size page]
   (let [response (raw-metric-page project-id metric page page-size)
         measures (:body response)
         parsed (json/read-str measures :key-fn keyword)
         this-page (:components parsed)]
     ;; (pprint (:paging parsed))
     (if (is-last-page (:paging parsed))
       this-page
       (reduce conj
               (fetch-metric project-id metric page-size (inc page))
               this-page)))))

(defn fetch-categorized-complexity [config]
  (let [project-id (:project-id config)
        page-size (:page-size config)
        orange (:complexity-orange-threshold config)
        red (:complexity-red-threshold config)
        components (fetch-metric project-id "complexity" page-size)
        interesting-part (map (fn [{:keys [key name]
                                    [{:keys [value]}] :measures}]
                                {:key key
                                 :name name
                                 :value value})
                              components)]
    (->> interesting-part
         (filter #(some? (:value %)))
         (filter #(not (str/includes? (:name %) "/")))
         (map #(update-in % [:value] read-string))
         (map #(assoc % :category (categorize-complexity orange red %)))
         (sort-by :value)
         (reverse)
         (group-by :category))))

(defn -main []
  (let [config (load-config)]
    (reset! auth-token (:token config))
    (let [complexity (fetch-categorized-complexity config)]
      ;; (pprint complexity)
      (println "Complexity:")
      (println "  green:" (count (:green complexity)))
      (println "  orange:" (count (:orange complexity)))
      (println "  red:" (count (:red complexity))))))
