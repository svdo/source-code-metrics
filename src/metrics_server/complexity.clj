(ns metrics-server.complexity
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as e]
            #_[clojure.spec.gen.alpha :as gen]
            [clojure.test.check.generators :as gen]))

(defn- extract-relevant-fields [{:keys             [key name]
                                 [{:keys [value]}] :measures}]
  {:key   key
   :name  name
   :value value})

(defn- value-string->number [entry]
  (update-in entry [:value] read-string))

(defn- has-a-value [entry]
  (some? (:value entry)))

(defn- categorize-complexity-number
  [entry {:keys [complexity/orange-threshold complexity/red-threshold]}]
  (let [value (:value entry)]
    (condp >= value
      orange-threshold :green
      red-threshold    :orange
      :red)))

(s/def :complexity/key string?)
(s/def :complexity/name string?)
(s/def :complexity/metric #{"complexity"})
(s/def :complexity/value
  (s/with-gen
    (s/and string? (comp int? read-string))
    #(gen/fmap str (s/gen #{5 15 25}))))

(s/def :complexity/measure
  (s/keys :req-un [:complexity/metric
                   :complexity/value]))
(s/def :complexity/measures
  (s/coll-of :complexity/measure))
(s/def :complexity/entry
  (s/keys :req-un [:complexity/key
                   :complexity/name
                   :complexity/measures]))
(s/def :complexity/entries
  (s/coll-of :complexity/entry))

(defn- check-valid [metrics-data]
  (if (s/valid? :complexity/entries metrics-data)
    metrics-data
    (throw (ex-info (e/expound-str :complexity/entries metrics-data)
                    (s/explain-data :complexity/entries metrics-data)))))

(defn categorize [metrics-data config]
  (let [interesting-part (map extract-relevant-fields (check-valid metrics-data))]
    (merge
      {:green [] :orange [] :red []}
      (->> interesting-part
           (filter has-a-value)
           (map value-string->number)
           (map #(assoc % :category (categorize-complexity-number % config)))
           (sort-by :value)
           (reverse)
           (group-by :category)))))
