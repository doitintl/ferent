(ns ferent.read-projects-and-calculate
  (:require
    [babashka.process :refer [check process]]
    [clojure.data.json :as json]
    [clojure.string :as str]
    [ferent.build-graph :refer [build-graph]]
    [ferent.metrics :refer [metrics]]
    [ferent.utils :refer [load-edn pfilter]]
    [sc.api :refer :all])
  (:import (java.util Date)))

(defn filter-by-env [all-projects]
  (let [require (re-pattern (or (System/getenv "REQUIRE") ""))
        projects-only-required (filter #(re-find require %) all-projects)
        exclude (re-pattern (or (System/getenv "EXCLUDE") "sys-|-fs-"))
        filtered (remove #(re-find exclude %) projects-only-required)]
    filtered))

(defn query-all-accessible-projects []
  (let [proj-triples (-> (process '[gcloud projects list "--limit" "1200"]) check :out slurp str/split-lines)
        _ (println "\"gcloud projects list\"  found " (count proj-triples))
        projects (map #(str/trim (first (str/split % #"\s+"))) (rest proj-triples))]
    projects))

(defn org-of [proj-id]
  (let [org-line (-> (process (conj '[gcloud projects get-ancestors] proj-id)) check :out slurp str/split-lines last)
        _ (println (str "gcloud projects get-ancestors " proj-id))
        org (first (str/split org-line #"\s+"))]
    org))

(defn service-accounts-in [proj-id]
  (let [cmd-line (str "gcloud iam service-accounts list --project " proj-id)
        _ (println cmd-line)
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
        _ (println cmd-line)
        j (-> cmd-line process check :out slurp json/read-str)
        members (apply concat (map #(% "members") (j "bindings")))
        service-account-pfx "serviceAccount:"
        sas-only (filter #(str/starts-with? % service-account-pfx) members)
        sa-emails (map #(subs % (count service-account-pfx)) sas-only)

        ]
    sa-emails))

(defn service-accounts-granted-role-by-projects [proj-ids]
  (into {} (pmap (fn [p] [p (service-accounts-granted-role-by p)]) proj-ids)))

(defn- projects-in-org [projects org]
  (assert (not (empty? (System/getenv "ORG_ID"))) "Must set ORG_ID env variable")
  (pfilter #(= (org-of %) org) projects))

(defn get-metrics [projs]
  (let [sa-granted-role-by-proj (service-accounts-granted-role-by-projects projs)
        sa-by-proj (service-accounts-in-projects projs)
        grph (build-graph sa-granted-role-by-proj sa-by-proj false)
        metrcs (metrics grph)]
    metrcs))

(defn filtered-projects []
  (let [projects-file (System/getenv "PROJECTS_FILE")
        loaded (if projects-file
                 (load-edn projects-file)
                 (query-all-accessible-projects))
        _ (println (count loaded) "projects unfiltered")

        projects (filter-by-env loaded)
        _ (println (count projects) "filtered projects")

        ]
    projects))

(defn -main []
  (println (Date.))
  (println (str "Env variables: " (str/join ", " (map #(str % ": " (or (System/getenv %) "-"))
                                                      ["ORG_ID" "REQUIRE" "EXCLUDE" "PROJECTS_FILE"]))))
  (time (let [projs (filtered-projects)
              projs-in-org (projects-in-org projs (System/getenv "ORG_ID"))
              _ (println (count projs-in-org) "projects in org")
              metrcs (get-metrics projs-in-org)]
          (println "Metrics:")
          (clojure.pprint/pprint metrcs)))
  )

