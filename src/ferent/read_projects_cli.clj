(ns ferent.read-projects-cli
  (:require
    [babashka.process :refer [check process]]
    [clojure.data.json :as json]
    [clojure.string :as str]
    [ferent.build-graph :refer [build-graph]]
    [ferent.metrics :refer [metrics]]
    [sc.api :refer :all])
  (:import (java.util Date)))

(def serviceA "serviceAccount:")

(defn accessible-projects []
  (let [

        proj-triples (-> (process '[gcloud projects list]) :out slurp str/split-lines)
        all-projects (map #(str/trim (first (str/split % #"\s+"))) (rest proj-triples))
        require (re-pattern(or (System/getenv "REQUIRE") ""))
        projects-only-required (filter #(re-find require %) all-projects)
        exclude (re-pattern(or (System/getenv "EXCLUDE") "sys-|-fs-"))
        projects-no-excluded (remove #(re-find exclude %) projects-only-required)
        ]
    projects-no-excluded))


(defn org-for-project [proj-id]
  (let [org-line (-> (process (conj '[gcloud projects get-ancestors] proj-id)) check :out slurp str/split-lines last)
        org (first (str/split org-line #"\s+"))]
    (println proj-id )
    org))

(defn service-accounts-in-project [proj-id]
  (let [lst (rest (
                    -> (process (str "gcloud iam service-accounts list --project " proj-id))
                       check :out slurp str/split-lines))
        emails (map #(first (take-last 2 (str/split % #"\s+"))) lst)]
    emails))

(defn service-accounts-in-projects [proj-ids]
  (into {} (for [p proj-ids] [p (service-accounts-in-project p)])))

(defn service-accounts-granted-role-by-project [proj-id]
  (let [cmd-line (str "gcloud projects get-iam-policy " proj-id
                      " --format json")
        process-output (process cmd-line)
        out-s ^String (-> process-output check :out slurp)
        j (json/read-str out-s)
        members (apply concat (map #(% "members") (j "bindings")))
        sas-only (filter #(str/starts-with? % serviceA) members)
        sa-emails (map #(subs % (count serviceA)) sas-only)]
    sa-emails))

(defn service-accounts-granted-role-by-projects [proj-ids]
  (into {} (for [p proj-ids] [p (service-accounts-granted-role-by-project p)])))


(defn- projects-in-org [projects org]
  (filter #(= (org-for-project %) org) projects))

(defn- list-projects []
  (assert (not (empty? (System/getenv "ORG_ID"))) "Must set ORG_ID env varable")
  (let [org (System/getenv "ORG_ID")
        projects (accessible-projects)
        in-org (projects-in-org projects org)

        ]
    in-org))

(println(new Date))
(println(time (str ".......\n"(str/join "\n" (list-projects)))))


(defn get-metrics [projs]
  (let [sa-granted-role-by-proj (service-accounts-granted-role-by-projects projs)
        sa-by-proj (service-accounts-in-projects projs)
        grph (build-graph sa-granted-role-by-proj sa-by-proj false)
        metrcs (metrics grph)]
    metrcs))



(defn -main []
  (println (str "Env variables: " (str/join ", " (map #(str % ": " (or (System/getenv %) "-")) ["ORG_ID" "REQUIRE" "EXCLUDE"]))))
  (let [projs (list-projects)
        metrcs (get-metrics projs)]
    (clojure.pprint/pprint metrcs)))




