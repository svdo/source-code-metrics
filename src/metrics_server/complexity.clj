(ns metrics-server.complexity
  (:require [clojure.string :as str]))

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

(defn categorize-complexity-number
  [entry {:keys [complexity-orange-threshold complexity-red-threshold]}]
  (let [value (:value entry)]
    (cond
      (< value complexity-orange-threshold) :green
      (< value complexity-red-threshold)    :orange
      :else                                 :red)))

(defn categorize [metrics-data config]
  (let [interesting-part (map extract-relevant-fields metrics-data)]
    (->> interesting-part
         (filter has-a-value)
         (filter is-a-file)
         (map value-string-to-number)
         (map #(assoc % :category (categorize-complexity-number % config)))
         (sort-by :value)
         (reverse)
         (group-by :category))))