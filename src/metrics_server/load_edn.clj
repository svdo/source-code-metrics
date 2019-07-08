(ns metrics-server.load-edn
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (throw (ex-info "I/O exception loading edn file" {:file source} e)))
    (catch RuntimeException e
      (throw (ex-info "Error parsing edn file" {:file source} e)))))

(defn load-edn! [source]
  (try
    (load-edn source)
    (catch Exception _ {})))
