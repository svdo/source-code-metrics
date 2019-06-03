(ns metrics-server.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.pprint :refer [pprint]]
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

(defn get-user-tokens []
  (let [response (client/get (url token-search) basic-auth)
        user-tokens (:body response)]
    (pprint (json/read-str user-tokens :key-fn keyword))))

(defn get-analyses []
  (let [response (client/get (url project-analyses-search)
                             (merge basic-auth
                                    {:query-params {:project project-id}}))
        analyses (:body response)]
    (pprint (json/read-str analyses :key-fn keyword))))

(defn get-component []
  (let [response (client/get (url measures-component)
                             (merge basic-auth
                                    {:query-params {:component project-id
                                                    :metricKeys "ncloc,complexity"}}))
        measures (:body response)]
    (pprint (json/read-str measures :key-fn keyword))))

(defn categorize [entry]
  (let [value (:value entry)]
    (cond
      (< value 9)  :green
      (< value 15) :orange
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
     (pprint (:paging parsed))
     (if (is-last-page (:paging parsed))
       this-page
       (reduce conj (fetch-metric project-id metric (inc page)) this-page)))))

(defn get-component-tree []
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

(defn -main
  [& args]
  #_(get-user-tokens)
  #_(get-analyses)
  #_(get-component)
  (let [tree (get-component-tree)]
    (pprint tree)
    (println "Total:" (count tree) "components.")))
