(ns metrics-server.config-test
  (:require [metrics-server.config :as config]
            [clojure.test :refer (deftest testing is)]))

(deftest config-test
  (testing "it can load a valid config file"
    ;; If this test fails, please make sure you have valid configuration
    ;; file, preferably by creating a `config.local.edn` file
    ;; (see `README.md`).
    (is (config/load-config))))
