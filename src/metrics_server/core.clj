(ns metrics-server.core
  (:gen-class)
  (:require [metrics-server.config :refer (load-config)]
            [metrics-server.sonar-fetcher :as sonar]
            [metrics-server.complexity :as complexity]))

(defn -main []
  (let [config (load-config)
        metrics-data (sonar/fetch-metric "complexity" config)
        complexity (complexity/categorize metrics-data config)]
    (println "Project key:" (:project-id config))
    (println "Complexity:")
    (println "  green:" (count (:green complexity)))
    (println "  orange:" (count (:orange complexity)))
    (println "  red:" (count (:red complexity)))))
