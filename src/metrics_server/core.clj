(ns metrics-server.core
  (:gen-class)
  (:require [clj-http.client :as client]
            ; [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.data.json :as json]))

(def token "<redacted>")
(def projectId "<redacted>")
(def basic-auth {:basic-auth (str token ":")})
(def page-size 100)

(defn url [endpoint]
  (str "<redacted>" endpoint))
(def token-search "/api/user_tokens/search")
(def project-search "/api/projects/search")
(def project-analyses-search "/api/project_analyses/search")
(def measures-component "/api/measures/component")
(def measures-component-tree "/api/measures/component_tree")

(def complexity-orange-threshold 9)
(def complexity-red-threshold 15)

(defn categorize [entry]
  (let [value (:value entry)]
    (cond
      (< value complexity-orange-threshold)  :green
      (< value complexity-red-threshold) :orange
      :else        :red)))

(defn raw-metric-page [project-id metric page page-size]
  (client/get (url measures-component-tree)
              (merge basic-auth
                     {:query-params {:component project-id
                                     :metricKeys metric
                                     :p page
                                     :ps page-size}})))

(defn is-last-page [{:keys [pageIndex pageSize total]}]
  (> (* pageIndex pageSize) total))

(defn fetch-metric
  ([project-id metric] (fetch-metric project-id metric 1))
  ([project-id metric page]
   (let [response (raw-metric-page project-id metric page page-size)
         measures (:body response)
         parsed (json/read-str measures :key-fn keyword)
         this-page (:components parsed)]
     ;; (pprint (:paging parsed))
     (if (is-last-page (:paging parsed))
       this-page
       (reduce conj (fetch-metric project-id metric (inc page)) this-page)))))

(defn categorize-complexity []
  (let [components (fetch-metric project-id "complexity")
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
         (map #(assoc % :category (categorize %)))
         (sort-by :value)
         (reverse)
         (group-by :category))))

(defn -main []
  (let [complexity (categorize-complexity)]
    ;; (pprint complexity)
    (println "Complexity:")
    (println "  green:" (count (:green complexity)))
    (println "  orange:" (count (:orange complexity)))
    (println "  red:" (count (:red complexity)))))
