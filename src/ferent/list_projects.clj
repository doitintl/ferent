(ns ferent.list-projects
  (:require [babashka.process :refer [check process]]
            [clojure.pprint :as pprint]
            [clojure.string :as str])

  (:import (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
           (com.google.api.client.json.gson GsonFactory)
           (com.google.api.services.cloudresourcemanager.v3 CloudResourceManager CloudResourceManager$Builder)
           (com.google.api.services.cloudresourcemanager.v3.model Project SearchProjectsResponse)
           (com.google.api.services.iam.v1 IamScopes)
           (com.google.auth.http HttpCredentialsAdapter)
           (com.google.auth.oauth2 GoogleCredentials)))
(set! *warn-on-reflection* true)

(def FILTER "NOT displayName=doitintl* AND NOT projectId=sys-*") ;;todo get from env and pass as param
(def ORG " gi")                                    ;todo get from env and pass as param

(def page-size 1000)                                         ;;todo set to 900
(defn- cloud-resource-manager-service []
  (let [credential (.createScoped (GoogleCredentials/getApplicationDefault) [IamScopes/CLOUD_PLATFORM])
        svc (.build                                         ;use .. method chaining
             (.setApplicationName
              (new CloudResourceManager$Builder
                   (GoogleNetHttpTransport/newTrustedTransport)
                   (GsonFactory/getDefaultInstance)
                   (new HttpCredentialsAdapter credential)) "project-listing"))]

    svc))

(defn- process-project [^String org-id ^Project proj]

  (let [org-of (fn [proj-id]
                 (let [org-line (-> (process (conj '[gcloud projects get-ancestors] proj-id)) check :out slurp str/split-lines last)
                       org (first (str/split org-line #"\s+"))]
                   org))
        project-id (.getProjectId proj)
        parent (.getParent proj)
        org-pfx "organizations/"
        org-pth (str org-pfx org-id)]

    (cond (= parent org-pth)
          project-id                                        ; in our org
          (not (str/starts-with? parent org-pfx))
          (cond (= org-id (org-of project-id))              ; in a folder
                project-id                                  ; in a folder in our org
                (empty? org-id)
                (throw (Exception. (str project-id " is in parent " parent " yet no org was found")))
                :else
                nil)                                        ;  in a folder, but not in our org

          :else                                             ;It is directly in an org, but not ours
          nil)))

(defn- one-page-query [^CloudResourceManager svc
                       ^String filter
                       ^String org-id
                       ^String tok]

  (let [^SearchProjectsResponse searched (.. svc (projects) (search) (setQuery filter) (setPageToken tok) (setPageSize (Integer. page-size)) execute)
        tok (.getNextPageToken searched)
        projs (.getProjects searched)
        _ (println "one-search-query found" (count projs))
        project-ids-with-nils (pmap (partial process-project org-id) projs)
        _ (println)
        proj-ids-in-org (remove nil? project-ids-with-nils)
        ]
    {:projects proj-ids-in-org :token tok}))

(defn- query-projects-in-org [^String filter ^String org-id]
  (let [paged-query-int
        (fn []                                              ; use params from closure
          (let [svc (cloud-resource-manager-service)]
            (loop [tok nil
                   accumulator []]
              (println "accumulator has" (count accumulator))

              (let [{projects-from-query :projects
                     tok-from-query      :token} (one-page-query svc filter org-id tok)
                    accumulated-projects (concat accumulator projects-from-query)
                    ]
                (if (nil? tok-from-query)
                  accumulated-projects
                  (recur tok-from-query  accumulated-projects))))))]
    (sort (paged-query-int))))

(time (let [projects (query-projects-in-org FILTER ORG)]

        (pprint/pprint projects)
        (pprint/pprint (count projects))))