(ns metrics-server.sonar-fetcher-test
  (:require [clojure.test :refer (deftest testing is)]
            [metrics-server.sonar-fetcher :as sonar]
            [clojure.data.json :as json]))

(def config
  {:sonar/project-id :dummy
   :fetch/page-size 2
   :sonar/token :dummy})

(defn create-content [page]
  (condp = page
    1 [{:a 1} {:b 2}]
    2 [{:c 3} {:d 4}]
    3 [{:e 5}]))

(defn test-page-getter [_ _ _ page page-size _]
  {:body
   (json/write-str {:paging {:pageIndex page, :pageSize page-size :total 5}
                    :components (create-content page)})})

(deftest sonar-fetcher-test
  (testing "it combines multiple pages"
    (is (= [{:a 1} {:b 2} {:c 3} {:d 4} {:e 5}]
           (sonar/fetch-file-tree-metric :dummy config 1 test-page-getter)))))
