(ns metrics-server.project-metrics
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [expound.alpha :as e]))

(defn- value-string-to-number [dict]
  (into {} (for [[k v] dict] [(keyword (str/replace (name k) "_" "-"))
                              (read-string v)])))

(defn- extract-relevant-fields [metric-dict]
  {(keyword (:metric metric-dict))
   (or (:value metric-dict) (:value (first (:periods metric-dict))))})

(s/def :metrics/value
  (s/and string? #_(comp (or int? float?) read-string)
         (s/or :int-string (comp int? read-string)
               :float-string (comp float? read-string))))
(s/def :metrics/period
  (s/keys :opt-un [:metrics/value]))
(s/def :metrics/periods (s/coll-of :metrics/period))
(s/def :metrics/measure
  (s/and
   (s/keys :req-un [::metric]
           :opt-un [:metrics/value
                    :metrics/periods])
   (s/or :measure-value #(:value %)
         :first-period-value #(:value (first (:periods %))))))
(s/def :metrics/measures (s/coll-of :metrics/measure))
(s/def :metrics/data
  (s/keys :req-un [:metrics/measures]))

(defn valid-data [metrics-data]
  (if (s/valid? :metrics/data metrics-data)
    metrics-data
    (throw (ex-info (e/expound-str :metrics/data metrics-data)
                    (s/explain-data :metrics/data metrics-data)))))

(defn metrics [metrics-data]
  (->> (:measures (valid-data metrics-data))
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
