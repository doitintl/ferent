(ns ferent.read-projects-and-calculate
  (:require
   [babashka.process :refer [check process]]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [ferent.build-graph :refer [build-graph]]
   [ferent.list-projects :refer [filtered-projects-in-org]]
   [ferent.metrics :refer [metrics]]
   [ferent.utils :refer [get-env load-edn]]))

(defn service-accounts-in [proj-id]
  (let [cmd-line (str "gcloud iam service-accounts list --project " proj-id)
        process-result (process cmd-line)
        checked (try (check process-result)
                     (catch Exception e
                       (do
                         (.println *err* (str "Exception at " cmd-line ": " (.getMessage e)))
                         nil)))
        lst (if (nil? checked)
              []
              (-> checked :out slurp str/split-lines rest))
        emails (map #(first (take-last 2 (str/split % #"\s+"))) lst)]
    emails))

(defn service-accounts-in-projects [proj-ids]
  (into {} (pmap (fn [p] [p (service-accounts-in p)]) proj-ids)))

(defn- service-accounts-granted-role-by [proj-id]
  (let [cmd-line (str "gcloud projects get-iam-policy " proj-id " --format json")
        j (-> cmd-line process check :out slurp json/read-str)
        members (apply concat (map #(% "members") (j "bindings")))
        service-account-pfx "serviceAccount:"
        sas-only (filter #(str/starts-with? % service-account-pfx) members)
        sa-emails (map #(subs % (count service-account-pfx)) sas-only)]

    sa-emails))

(defn service-accounts-granted-role-by-projects [proj-ids]
  (into {} (pmap (fn [p] [p (service-accounts-granted-role-by p)]) proj-ids)))

(defn get-metrics [projs]
  (let [sa-granted-role-by-proj (service-accounts-granted-role-by-projects projs)
        sa-by-proj (service-accounts-in-projects projs)
        grph (build-graph sa-granted-role-by-proj sa-by-proj false)
        metrcs (metrics grph)]
    metrcs))

(defn load-projects []
  (let [projects-file (get-env "PROJECTS_FILE")
        loaded (if projects-file
                 (load-edn projects-file)
                 (filtered-projects-in-org))]
    loaded))

(defn -main []
  (let [start (System/currentTimeMillis)
        projects (load-projects)
        metrcs (get-metrics projects)]
    (clojure.pprint/pprint metrcs)
    (.println *err* (str "Total time "
                         (Math/round (double (/ (- (System/currentTimeMillis) start) 1000)))
                         " seconds"))))

