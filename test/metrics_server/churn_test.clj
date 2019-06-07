(ns metrics-server.churn-test
  (:require [clojure.test :refer (deftest testing is)]
            [metrics-server.load-edn :refer (load-edn!)]
            [metrics-server.churn :refer (summarize)]))

(def sample-commit-data
  (load-edn! "./test/metrics_server/sample_commit_data.edn"))

(deftest number-of-non-merge-commits
  (is (= 4 (:count (summarize sample-commit-data)))))

(deftest number-of-lines-added
  (is (= 919 (:lines-added (summarize sample-commit-data)))))

(deftest number-of-lines-deleted
  (is (= 589 (:lines-deleted (summarize sample-commit-data)))))
