(ns metrics-server.core
  (:gen-class)
  (:require [metrics-server.config :refer (load-config)]
            [metrics-server.sonar-fetcher :as sonar]
            [metrics-server.project-metrics :as project]
            [metrics-server.complexity :as complexity]))

(defn -main []
  (let [config (load-config)
        project-data (sonar/fetch-project-metrics ["coverage" "new_coverage"] config)
        project-metrics (project/metrics project-data)
        complexity-data (sonar/fetch-file-tree-metric "complexity" config)
        complexity (complexity/categorize complexity-data config)]
    (println "Project key:" (:project-id config))
    (println "Coverage:")
    (println "  overall:" (:coverage project-metrics))
    (println "  new code:" (:new-coverage project-metrics))
    (println "Complexity:")
    (println "  green:" (count (:green complexity)))
    (println "  orange:" (count (:orange complexity)))
    (println "  red:" (count (:red complexity)))))
