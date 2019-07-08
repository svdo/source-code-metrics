(ns metrics-server.gitlab-fetcher-test
  (:require [clojure.test :refer (deftest testing is)]
            [metrics-server.gitlab-fetcher :as gitlab]
            [clojure.data.json :as json]))

(defn create-response [content link]
  {:headers {"Link"
             (str "<https://first>; rel=\"first\""
                  (if link
                    (format ", <https://%s>; rel=\"next\"" link)
                    ""))}
   :body    (json/write-str content)})

(defn test-page-getter [_ _ _ url]
  (condp = url
    "https://first" (create-response [{:a 1}] "next")
    "https://next"  (create-response [{:b 2}] "last")
    "https://last"  (create-response [{:c 3}] nil)))

(deftest gitlab-fetcher-test
  (testing "it combines multiple pages"
    (let [parse-measures #'gitlab/parse-measures]
     (is (= [{:a 1} {:b 2} {:c 3}]
            (parse-measures :dummy :dummy :dummy :dummy "https://first" test-page-getter))))))
