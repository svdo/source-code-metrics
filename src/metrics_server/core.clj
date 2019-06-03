(ns metrics-server.core
  (:gen-class)
  (:require [clj-http.client :as client]
            ; [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def config (atom nil))

(defn basic-auth [] {:basic-auth (str (:token @config) ":")})
(defn url [endpoint] (str "<redacted>" endpoint))
(def measures-component-tree "/api/measures/component_tree")

(defn categorize [entry]
  (let [value (:value entry)]
    (cond
      (< value (:complexity-orange-threshold @config)) :green
      (< value (:complexity-red-threshold @config))    :orange
      :else                                            :red)))

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
  ([project-id metric] (fetch-metric project-id metric 1 (:page-size @config)))
  ([project-id metric page page-size]
   (let [response (raw-metric-page project-id metric page page-size)
         measures (:body response)
         parsed (json/read-str measures :key-fn keyword)
         this-page (:components parsed)]
     ;; (pprint (:paging parsed))
     (if (is-last-page (:paging parsed))
       this-page
       (reduce conj
               (fetch-metric project-id metric (inc page) page-size)
               this-page)))))

(defn categorize-complexity []
  (let [components (fetch-metric (:project-id @config) "complexity")
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

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))

(defn -main []
  (reset! config (load-edn "config.edn"))
  (let [complexity (categorize-complexity)]
    ;; (pprint complexity)
    (println "Complexity:")
    (println "  green:" (count (:green complexity)))
    (println "  orange:" (count (:orange complexity)))
    (println "  red:" (count (:red complexity)))))
