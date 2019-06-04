(ns metrics-server.core
  (:gen-class)
  (:require [metrics-server.config :refer (load-config)]
            [metrics-server.sonar-fetcher :as sonar]
            [metrics-server.project-metrics :as project]
            [metrics-server.complexity :as complexity]))

(defn -main []
  (let [config (load-config)
        project-data (sonar/fetch-project-metrics ["files" "complexity" "coverage" "new_coverage"] config)
        project-metrics (project/metrics project-data)
        complexity-data (sonar/fetch-file-tree-metric "complexity" config)
        complexity (complexity/categorize complexity-data config)]
    (println "SonarQube project key:" (:sonar-project-id config))
    (println "Complexity:")
    (println "  Number of files:" (:files project-metrics))
    (println "  Sum of complexity:" (:complexity project-metrics))
    (println "  green:" (count (:green complexity)))
    (println "  orange:" (count (:orange complexity)))
    (println "  red:" (count (:red complexity)))
    (println "Coverage:")
    (println "  overall:" (format "%.1f" (:coverage project-metrics)))
    (println "  new code:" (format "%.1f" (:new-coverage project-metrics)))))
