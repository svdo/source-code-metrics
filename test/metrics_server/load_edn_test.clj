(ns metrics-server.load-edn-test
  (:require [clojure.test :refer (deftest testing is)]
            [metrics-server.load-edn :as edn]))

(deftest load-edn-test
  (testing "it throws on invalid input"
    (is (thrown? RuntimeException (edn/load-edn "nonexisting"))))

  (testing "it ignores invalid input"
    (is (= {} (edn/load-edn! "nonexisting")))))
