(ns ferent.service-account-info
  (:require
   [babashka.process :refer [check process]]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [com.climate.claypoole :as cp]
   [ferent.utils :refer [thread-count]]))

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
  (into {} (cp/pmap (cp/threadpool thread-count) (fn [p] [p (service-accounts-in p)]) proj-ids)))

(defn- service-accounts-granted-role-by [proj-id]
  (let [cmd-line (str "gcloud projects get-iam-policy " proj-id " --format json")
        j (-> cmd-line process check :out slurp json/read-str)
        members (apply concat (map #(% "members") (j "bindings")))
        service-account-pfx "serviceAccount:"
        sas-only (filter #(str/starts-with? % service-account-pfx) members)
        sa-emails (map #(subs % (count service-account-pfx)) sas-only)]

    sa-emails))

(defn service-accounts-granted-role-by-projects [proj-ids]
  (into {} (cp/pmap (cp/threadpool thread-count) (fn [p] [p (service-accounts-granted-role-by p)]) proj-ids)))



