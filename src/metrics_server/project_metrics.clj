(ns metrics-server.project-metrics
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [expound.alpha :as e]
            [clojure.test.check.generators :as gen]))

(defn- value-string->number [dict]
  (into {} (for [[k v] dict] [(keyword (str/replace (name k) "_" "-"))
                              (read-string v)])))

(defn- extract-relevant-fields [metric-dict]
  {(keyword (:metric metric-dict))
   (or (:value metric-dict) (:value (first (:periods metric-dict))))})

(def ^:private value-gen
  (gen/one-of
    [(gen/double* {:infinite? false :NaN? false :min 0.0 :max 100.0})
     (gen/large-integer* {:min 0 :max 100})]))

(s/def :metrics/value
  (s/with-gen
    (s/and string? #_(comp (or int? float?) read-string)
           (s/or :int-string (comp int? read-string)
                 :float-string (comp float? read-string)))
    #(gen/fmap str value-gen)))
(s/def :metrics/metric #{"files" "complexity" "coverage" "ncloc" "new_coverage" "vulnerabilities"})
(s/def :metrics/period
  (s/keys :opt-un [:metrics/value]))
(s/def :metrics/periods (s/coll-of :metrics/period))
(s/def :metrics/measure
  (s/and
   (s/keys :req-un [:metrics/metric]
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
       (map value-string->number)
       (reduce merge)))
