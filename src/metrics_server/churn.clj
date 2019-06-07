(ns metrics-server.churn)

(defn summarize [commit-data]
  {:count (count commit-data)})
