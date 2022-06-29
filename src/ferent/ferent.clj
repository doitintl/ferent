(ns ferent.ferent
  (:require
   [babashka.process :refer [check process]]
   [clojure.tools.cli :as cli]
   [ferent.build-graph :refer [build-graph]]
   [ferent.metrics :refer [metrics]]
   [ferent.query-projects :refer [filtered-projects-in-org]]
   [ferent.service-account-info :refer [service-accounts-granted-role-by-projects service-accounts-in-projects]]
   [ferent.utils :refer [load-edn] :as utils]))

(defn- build-metrics [projs]
  (metrics (build-graph (service-accounts-granted-role-by-projects projs)
                        (service-accounts-in-projects projs)  )))

(defn- load-projects [{:keys [projects-file org-id filter]}]
  (if projects-file
    (load-edn projects-file)
    (filtered-projects-in-org filter org-id)))

(defn- check-org! [org-id]
  (when (some? org-id)
    (Long/parseLong org-id)                                 ;Just assert that it has a numerical format
    (check (process (concat '(gcloud organizations describe) [org-id])))))

(defn do-all [params]
  (.println *err* (str "Params: " params))
  (check-org! (:org-id params))
  (-> params load-projects build-metrics))

(defn do-all-and-print [params]
  (utils/timer "Total" (clojure.pprint/pprint (do-all params))))

(def cli-options
  [["-o" "--org-id ORG_ID" "Numerical org id, mandatory"]
   ["-q" "--query-filter QUERY_FILTER" "Query filter" :default "NOT projectId=sys-*"]
   ["-f" "--projects-file PROJECT_FILE" "Project file" :default nil]
   ["-h" "--help"]])

(defn -main [& args]
  (do-all-and-print (:options (cli/parse-opts args cli-options))))

(comment (do-all {:org-id "976583563296"
                  :filter "NOT displayName=doit* AND NOT projectId=sys-*"}))

