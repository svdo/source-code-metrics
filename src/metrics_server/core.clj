(ns metrics-server.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.data.json :as json]))

(def token "<redacted>")
(def projectId "<redacted>")
(def basic-auth {:basic-auth (str token ":")})

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
                                    {:query-params {:project projectId}}))
        analyses (:body response)]
    (pprint (json/read-str analyses :key-fn keyword))))

(defn get-component []
  (let [response (client/get (url measures-component)
                             (merge basic-auth
                                    {:query-params {:component projectId
                                                    :metricKeys "ncloc,complexity"}}))
        measures (:body response)]
    (pprint (json/read-str measures :key-fn keyword))))

(defn categorize [entry]
  (let [value (:value entry)]
    (cond
      (< value 9) :green
      (< value 15) :orange
      :else :red)))

(defn get-component-tree []
  (let [response (client/get (url measures-component-tree)
                             (merge basic-auth
                                    {:query-params {:component projectId
                                                    :metricKeys "complexity"
                                                    :ps 499}}))
        measures (:body response)
        parsed (json/read-str measures :key-fn keyword)
        components (:components parsed)
        as-list (map (fn [{:keys [key name]
                           [{:keys [value]}] :measures}]
                       {:key key
                        :name name
                        :value value})
                     components)]
    (->> as-list
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
