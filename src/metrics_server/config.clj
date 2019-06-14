(ns metrics-server.config
  (:require [metrics-server.load-edn :refer (load-edn load-edn!)]))

(defn load-config []
  (let [config       (load-edn "config.edn")
        local-config (load-edn! "config.local.edn")]
    (merge config local-config)))