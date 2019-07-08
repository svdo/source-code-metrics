(ns metrics-server.complexity-test
  (:require [clojure.test :refer (deftest testing is)]
            [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer (defspec)]
            [metrics-server.complexity :as complexity]))

(def config {:complexity/orange-threshold 10
             :complexity/red-threshold    20})

(defspec complexity-spec 100
  (prop/for-all
    [sample (s/gen :complexity/entries)]
    (let [categorized (complexity/categorize sample config)]
      (and (:green categorized)
           (:orange categorized)
           (:red categorized)))))

(deftest complexity-test
  (testing "complexity boundaries"
    (let [categorize-complexity-number #'complexity/categorize-complexity-number]
      (is (= :red (categorize-complexity-number {:value 25} config)))
      (is (= :orange (categorize-complexity-number {:value 15} config)))
      (is (= :green (categorize-complexity-number {:value 5} config)))))

  (testing "it throws on invalid input"
    (is (thrown? RuntimeException (complexity/categorize [{}] config)))))
