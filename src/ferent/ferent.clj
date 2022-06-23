(ns ferent.ferent
  (:require
   [babashka.process :refer [check process]]
   [ferent.build-graph :refer [build-graph]]
   [ferent.list-projects :refer [filtered-projects-in-org]]
   [ferent.service-account-info :refer [service-accounts-granted-role-by-projects service-accounts-in-projects]]
   [ferent.metrics :refer [metrics]]
   [ferent.utils :refer [get-env load-edn]]))

(defn build-metrics [projs]
  (.println *err* (str "Getting service accounts for each of the " (count projs) " projects"))
  (let [sa-granted-role-by-proj (service-accounts-granted-role-by-projects projs)
        sa-by-proj (service-accounts-in-projects projs)
        grph (build-graph sa-granted-role-by-proj sa-by-proj false)]
    (metrics grph)))

(defn load-projects []
  (let [projects-file (get-env "PROJECTS_FILE")
        loaded (if projects-file
                 (load-edn projects-file)
                 (filtered-projects-in-org))]
    loaded))

(defn- check-org []
  (when-let [org-id (get-env "ORG_ID")]
    (let [cmd-line (concat '(gcloud organizations describe) [org-id])]
      (when (nil? (check (process cmd-line)))
        (throw (Exception. (str "Org " org-id " not accessible")))))))

(defn -main []
  (check-org)
  (let [start (System/currentTimeMillis)
        projects (load-projects)
        metrcs (build-metrics projects)]
    (clojure.pprint/pprint metrcs)
    (.println *err* (str "Total time "
                         (Math/round (double (/ (- (System/currentTimeMillis) start) 1000)))
                         " seconds"))))

