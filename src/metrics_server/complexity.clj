(ns metrics-server.complexity)

(defn extract-relevant-fields [{:keys             [key name]
                                [{:keys [value]}] :measures}]
  {:key   key
   :name  name
   :value value})

(defn value-string-to-number [entry]
  (update-in entry [:value] read-string))

(defn has-a-value [entry]
  (some? (:value entry)))

(defn categorize-complexity-number
  [entry {:keys [complexity-orange-threshold complexity-red-threshold]}]
  (let [value (:value entry)]
    (condp < value
      complexity-orange-threshold :green
      complexity-red-threshold    :orange
      :red)))

(defn categorize [metrics-data config]
  (let [interesting-part (map extract-relevant-fields metrics-data)]
    (->> interesting-part
         (filter has-a-value)
         (map value-string-to-number)
         (map #(assoc % :category (categorize-complexity-number % config)))
         (sort-by :value)
         (reverse)
         (group-by :category))))