(ns ferent.ferent
  (:require
   [babashka.process :refer [check process]]
   [clojure.tools.cli :as cli]
   [ferent.build-graph :refer [build-graph]]
   [ferent.metrics :refer [metrics]]
   [ferent.query-projects :refer [filtered-projects-in-org]]
   [ferent.service-account-info :refer [service-accounts-granted-role-by-projects service-accounts-in-projects]]
   [ferent.utils :refer [get-env load-edn] :as utils]))

(defn- build-metrics [projs]
  (metrics (build-graph (service-accounts-granted-role-by-projects projs)
                        (service-accounts-in-projects projs))))

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

  (utils/timer "Total time"
               (-> params load-projects build-metrics)))

(defn do-all-and-print [params]
  (let [result (do-all params)]
    (.println *err* (str "
============================================
RESULT: Dependency metrics for projects in org that grant permissions to service accounts from other projects in the org."))

    (clojure.pprint/pprint result)))

(def cli-options
  [["-o" "--org-id ORG_ID" "Numerical org id, mandatory"]
   ["-q" "--filter QUERY_FILTER" "Query filter" :default "NOT projectId=sys-*"]
   ["-p" "--projects-file PROJECT_FILE" "Project file" :default nil]
   ["-h" "--help"]])

(defn -main [& args]
  (let [opts (cli/parse-opts args cli-options)
        errors (:errors opts)]
    (when errors
      (throw (IllegalArgumentException. (str errors))))
    (do-all-and-print (:options opts))))

(comment (do-all {:org-id (get-env "ORG_ID" true)
                  :filter "NOT displayName=doit* AND NOT projectId=sys-*"}))

