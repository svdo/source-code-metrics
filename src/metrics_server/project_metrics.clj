(ns metrics-server.project-metrics
  (:require [clojure.string :as str]))

(defn- value-string-to-number [dict]
  (into {} (for [[k v] dict] [(keyword (str/replace (name k) "_" "-"))
                              (read-string v)])))

(defn- extract-relevant-fields [metric-dict]
  {(keyword (:metric metric-dict)) (or (:value metric-dict) (:value (first (:periods metric-dict))))})

(defn metrics [metrics-data]
  (->> (:measures metrics-data)
       (map extract-relevant-fields)
       (map value-string-to-number)
       (reduce merge)))

(comment
  (def sample-1
    {:id        "sample-1-id"
     :key       "sample-1-key"
     :name      "Sample 1 Name"
     :qualifier "SA1"
     :measures  [{:metric  "new_coverage"
                  :periods [{:index     1
                             :value     "89.6428571428571"
                             :bestValue false}]}
                 {:metric  "ncloc"
                  :value   "5044"
                  :periods [{:index 1
                             :value "210"}]}]})
  (def sample-2
    {:id        "sample-2-id"
     :key       "sample-2-key"
     :name      "Sample 2 Name"
     :qualifier "SA2"
     :measures  [{:metric  "new_coverage"
                  :periods [{:index     1
                             :value     "89.6428571428571"
                             :bestValue false}]}
                 {:metric    "coverage"
                  :value     "73.5"
                  :periods   [{:index     1
                               :value     "33.6"
                               :bestValue false}]
                  :bestValue false}]})

  (metrics sample-1)
  (metrics sample-2))
