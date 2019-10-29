(ns metrics-server.core
  (:gen-class)
  (:require [metrics-server.config :refer (load-config)]
            [metrics-server.sonar-fetcher :as sonar]
            [metrics-server.project-metrics :as project]
            [metrics-server.complexity :as complexity]
            [metrics-server.gitlab-fetcher :as gitlab]
            [metrics-server.churn :as churn]))

(defn- format-if-present [flt]
  (if flt
    (format "%.1f" flt)
    "n/a"))

(defn- build-complexity-measures [config]
  (-> (sonar/fetch-file-tree-metric "complexity" config)
      (complexity/categorize config)))

(defn- print-project-report [config project]
  (let [project-with-config (merge (dissoc config :report/projects) project)
        sonar-data          (sonar/fetch-project-metrics
                             ["files" "complexity" "coverage" "new_coverage" "vulnerabilities"]
                             project-with-config)
        sonar-metrics       (project/metrics sonar-data)
        complexity          (build-complexity-measures project-with-config)
        commit-data         (gitlab/fetch-commit-details project-with-config)
        churn               (churn/summarize commit-data)]
    (println "--------------------------------------------------------------")
    (println "Project:"
             (:name sonar-data)
             (str "(" (:sonar/project-id project-with-config) ")"))
    (println "Complexity:")
    (println "  Number of files:" (:files sonar-metrics))
    (println "  Sum of complexity:" (:complexity sonar-metrics))
    (println "  green:" (count (:green complexity)))
    (println "  orange:" (count (:orange complexity)))
    (println "  red:" (count (:red complexity)))
    (println "Coverage:")
    (println "  overall:" (format-if-present (:coverage sonar-metrics)))
    (println "  new code:" (format-if-present (:new-coverage sonar-metrics)))
    (println "Security:")
    (println "  vulnerabilities:" (:vulnerabilities sonar-metrics))
    (println "Churn:")
    (println "  number of commits:" (format "%d" (:count churn)))
    (println "  number of lines added:" (format "%d" (:lines-added churn)))
    (println "  number of lines deleted:" (format "%d" (:lines-deleted churn)))))

(defn -main []
  (let [config   (load-config)
        projects (:report/projects config)]
    (doall (map (partial print-project-report config) projects))))
