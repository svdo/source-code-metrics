(ns metrics-server.config
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))

(defn load-edn! [source]
  (try
    (load-edn source)
    (catch Exception e {})))

(defn load-config []
  (let [config (load-edn "config.edn")
        local-config (load-edn! "config.local.edn")]
    (merge config local-config)))