(ns metrics-server.churn)

(defn- extract-relevant-fields [{{:keys [additions deletions]} :stats}]
  {:additions additions
   :deletions deletions})

(defn- is-not-merge-commit? [commit-entry]
  (= 1 (count (:parent_ids commit-entry))))

(defn summarize [commit-data]
  (let [interesting-part (->> commit-data
                              (filter is-not-merge-commit?)
                              (map extract-relevant-fields))
        lines-added      (reduce + (map :additions interesting-part))
        lines-deleted    (reduce + (map :deletions interesting-part))]
    {:count         (count interesting-part)
     :lines-added   lines-added
     :lines-deleted lines-deleted}))
