(ns ferent.service-account-info
  (:require
    [babashka.process :refer [check process]]
    [clojure.data.json :as json]
    [clojure.string :as str]
    [com.climate.claypoole :as cp]
    [ferent.utils :refer [thread-count]]))

(defn- service-accounts-in [proj-id]
  (let [cmd-line (str "gcloud iam service-accounts list --project " proj-id)
        checked (try (check (process cmd-line))
                     (catch Exception e
                       (do
                         (.println *err* (str "Exception at " cmd-line ": " (.getMessage e)))
                         nil)))
        emails (map #(first (take-last 2 (str/split % #"\s+")))
                    (if (nil? checked)
                      []
                      (-> checked :out slurp str/split-lines rest)))]
    emails))

(defn service-accounts-in-projects [proj-ids]
  (into {} (cp/pmap (cp/threadpool thread-count) (fn [p] [p (service-accounts-in p)]) proj-ids)))

(defn- service-accounts-granted-role-by [proj-id]
  (let [json (-> (str "gcloud projects get-iam-policy " proj-id " --format json") process check :out slurp json/read-str)
        service-account-pfx "serviceAccount:"]
    (map #(subs % (count service-account-pfx))
         (filter #(str/starts-with? % service-account-pfx)
                 (apply concat (map #(% "members") (json "bindings")))))))

(defn service-accounts-granted-role-by-projects [proj-ids]
  (into {} (cp/pmap (cp/threadpool thread-count) (fn [p] [p (service-accounts-granted-role-by p)]) proj-ids)))



