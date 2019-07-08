(ns metrics-server.project-metrics-test
  (:require [clojure.test :refer (deftest testing is)]
            [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
            [metrics-server.project-metrics :as pm]
            [clojure.test.check.clojure-test :refer (defspec)]
            [clojure.test.check.properties :as prop]
            [clojure.string :as str]))

(defn get-metrics [measures]
  (map :metric measures))

(defn keyword->metric-name [kwd]
  (str/replace (name kwd) "-" "_"))

(defspec project-metrics-spec 100
         (prop/for-all
           [data (s/gen :metrics/data)]
           (let [result (pm/metrics data)]
             (let [requested-metrics (get-metrics (:measures data))
                   actual-metrics-keys (keys result)
                   actual-metrics (map keyword->metric-name actual-metrics-keys)]
               (= (set actual-metrics)
                  (set requested-metrics))))))

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

(deftest project-metrics-test
  (testing "sample 1"
    (is (= (pm/metrics sample-1)
           {:new-coverage 89.6428571428571
            :ncloc 5044})))

  (testing "sample 2"
    (is (= (pm/metrics sample-2)
           {:new-coverage 89.6428571428571
            :coverage 73.5}))))
