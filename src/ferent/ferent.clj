(ns ferent.ferent
  (:require
   [babashka.process :refer [check process]]
   [clojure.tools.cli :as cli]
   [ferent.build-graph :refer [build-graph]]
   [ferent.metrics :refer [metrics]]
   [ferent.query-projects :refer [filtered-projects-in-org]]
   [ferent.service-account-info :refer [service-accounts-granted-role-by-projects service-accounts-in-projects]]
   [ferent.utils :refer [load-edn]]))

(defn build-metrics [projs]
  (.println *err* (str "Getting service accounts for each of the " (count projs) " projects"))
  (let [sa-granted-role-by-proj (service-accounts-granted-role-by-projects projs)
        sa-by-proj (service-accounts-in-projects projs)
        grph (build-graph sa-granted-role-by-proj sa-by-proj false)]
    (metrics grph)))

(defn load-projects [{:keys [projects-file org-id filter]}]
  (if projects-file
    (load-edn projects-file)
    (filtered-projects-in-org filter org-id)))

(defn- check-org! [org-id]
  (when (some? org-id)
    (Long/parseLong org-id)                                 ;assert number passed
    (check (process (concat '(gcloud organizations describe) [org-id])))))

(defn do-all [params]
  (.println *err* (str "Params: " params))
  (check-org! (:org-id params))
  (-> params load-projects build-metrics))

(defn do-all-and-print [params]                             ;todo copy time macro and alter it as needed
  (let [start (System/currentTimeMillis)]
    (clojure.pprint/pprint (do-all params))
    (.println *err* (str "Total time "
                         (Math/round (double (/ (- (System/currentTimeMillis) start) 1000)))
                         " seconds"))))

(def cli-options
  [["-o" "--org-id ORG_ID" "Numerical org id, mandatory"]
   ["-q" "--query-filter QUERY_FILTER" "Query filter" :default "NOT projectId=sys-*"]
   ["-f" "--projects-file PROJECT_FILE" "Project file" :default nil]
   ["-h" "--help"]])

(defn -main [& args]
  (do-all-and-print (:options (cli/parse-opts args cli-options))))



