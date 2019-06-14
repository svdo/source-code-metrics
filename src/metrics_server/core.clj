(ns metrics-server.core
  (:gen-class)
  (:require [metrics-server.config :refer (load-config)]
            [metrics-server.sonar-fetcher :as sonar]
            [metrics-server.project-metrics :as project]
            [metrics-server.complexity :as complexity]
            [metrics-server.gitlab-fetcher :as gitlab]
            [metrics-server.churn :as churn]))

(defn format-if-present [flt]
  (if flt
    (format "%.1f" flt)
    "n/a"))

(defn -main []
  (let [config          (load-config)
        sonar-data      (sonar/fetch-project-metrics ["files" "complexity" "coverage" "new_coverage"] config)
        sonar-metrics   (project/metrics sonar-data)
        complexity-data (sonar/fetch-file-tree-metric "complexity" config)
        complexity      (complexity/categorize complexity-data config)
        commit-data     (gitlab/fetch-commit-details config)
        churn           (churn/summarize commit-data)]
    (println "SonarQube project key:" (:sonar-project-id config))
    (println "Complexity:")
    (println "  Number of files:" (:files sonar-metrics))
    (println "  Sum of complexity:" (:complexity sonar-metrics))
    (println "  green:" (count (:green complexity)))
    (println "  orange:" (count (:orange complexity)))
    (println "  red:" (count (:red complexity)))
    (println "Coverage:")
    (println "  overall:" (format-if-present (:coverage sonar-metrics)))
    (println "  new code:" (format-if-present (:new-coverage sonar-metrics)))
    (println "Churn:")
    (println "  number of commits:" (format "%d" (:count churn)))
    (println "  number of lines added:" (format "%d" (:lines-added churn)))
    (println "  number of lines deleted:" (format "%d" (:lines-deleted churn)))))
