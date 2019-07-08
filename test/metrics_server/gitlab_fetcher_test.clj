(ns metrics-server.gitlab-fetcher-test
  (:require [clojure.test :refer (deftest testing is)]
            [metrics-server.gitlab-fetcher :as gitlab]
            [clojure.data.json :as json]))

(def config
  {:gitlab/project-id :dummy
   :gitlab/token :dummy})

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
    (is (= [{:a 1} {:b 2} {:c 3}]
           (gitlab/fetch-commit-details :dummy :dummy config
                                        "https://first"
                                        test-page-getter)))))
