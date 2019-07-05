(ns metrics-server.churn
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as e]))

(defn- extract-relevant-fields [{{:keys [additions deletions]} :stats}]
  {:additions additions
   :deletions deletions})

(defn- is-not-merge-commit? [commit-entry]
  (= 1 (count (:parent_ids commit-entry))))

(s/def :commit/stats
  (s/keys :req-un [::additions ::deletions]))
(s/def :commit/entry
  (s/keys :req-un [::parent_ids :commit/stats]))
(s/def :commit/entries
  (s/coll-of :commit/entry))

(defn- check-valid [commit-data]
  (if (s/valid? :commit/entries commit-data)
    :ok
    (throw (ex-info (e/expound-str :commit/entries commit-data)
                    (s/explain-data :commit/entries commit-data)))))

(defn summarize [commit-data]
  (check-valid commit-data)
  (let [interesting-part (->> commit-data
                              (filter is-not-merge-commit?)
                              (map extract-relevant-fields))
        lines-added      (reduce + (map :additions interesting-part))
        lines-deleted    (reduce + (map :deletions interesting-part))]
    {:count         (count interesting-part)
     :lines-added   lines-added
     :lines-deleted lines-deleted}))
