(ns metrics-server.churn-test
  (:require [clojure.test :refer (deftest testing is)]
            [metrics-server.load-edn :refer (load-edn!)]
            [metrics-server.churn :refer (summarize)]))

(def sample-commit-data
  (load-edn! "./test/metrics_server/sample_commit_data.edn"))

(deftest number-of-commits
  (is (= 8 (:count (summarize sample-commit-data)))))
