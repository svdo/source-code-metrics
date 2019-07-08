(ns metrics-server.complexity-test
  (:require [clojure.test :refer (deftest testing is)]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [metrics-server.complexity :as complexity]))

(def config {:complexity/orange-threshold 10
             :complexity/red-threshold    20})

(deftest complexity-test
  (testing "complexity boundaries"
    (let [categorize-complexity-number #'complexity/categorize-complexity-number]
      (is (= :red (categorize-complexity-number {:value 25} config)))
      (is (= :orange (categorize-complexity-number {:value 15} config)))
      (is (= :green (categorize-complexity-number {:value 5} config)))))

  (testing "it contains green"
    (let [sample (gen/generate (s/gen :complexity/entries))]
      (is (:green (complexity/categorize sample config)))))

  (testing "it contains orange"
    (let [sample (gen/generate (s/gen :complexity/entries))
          categorized (complexity/categorize sample config)]
      (is (:orange categorized))))

  (testing "it contains red"
    (let [sample (gen/generate (s/gen :complexity/entries))
          categorized (complexity/categorize sample config)]
      (is (:red categorized)))))
