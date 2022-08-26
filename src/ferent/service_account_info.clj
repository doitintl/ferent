"This namespace holds the API-access to info about the service accounts
to which each project gives permissions, and the org that each is in."

(ns ferent.service-account-info
  (:require
   [babashka.process :refer [check process]]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [com.climate.claypoole :as cp]
   [ferent.utils :refer [thread-count]]))

(defn- service-accounts-in [proj-id]
  "The service accounts in the given project."
  (let [cmd-line (str "gcloud iam service-accounts list --project " proj-id)
        checked (try (check (process cmd-line))
                     (catch Exception e
                       (do
                         (.println *err* (str "Exception at " cmd-line ": " (.getMessage e)))
                         nil)))]
    (map #(first (take-last 2 (str/split % #"\s+")))
         (if (nil? checked)
           []
           (-> checked :out slurp str/split-lines rest)))))

(defn service-accounts-in-projects [proj-ids]
  "The service accounts in these projects."
  (into {} (cp/pmap (cp/threadpool thread-count) (fn [p] [p (service-accounts-in p)]) proj-ids)))

(defn- service-accounts-granted-role-by [proj-id]
  "The service accounts to which this project grants a role."
  (let [json (-> (str "gcloud projects get-iam-policy " proj-id " --format json") process check :out slurp json/read-str)
        service-account-pfx "serviceAccount:"]
    (map #(subs % (count service-account-pfx))
         (filter #(str/starts-with? % service-account-pfx)
                 (apply concat (map #(% "members") (json "bindings")))))))

(defn service-accounts-granted-role-by-projects [proj-ids]
  "The service accounts to which these projects grant a role."
  (into {} (cp/pmap (cp/threadpool thread-count) (fn [p] [p (service-accounts-granted-role-by p)]) proj-ids)))



