(ns ferent.list-projects
  (:require [babashka.process :refer [check process]]
            [clojure.string :as str]
            [ferent.utils :refer [get-env]])

  (:import (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
           (com.google.api.client.json.gson GsonFactory)
           (com.google.api.services.cloudresourcemanager.v3 CloudResourceManager CloudResourceManager$Builder)
           (com.google.api.services.cloudresourcemanager.v3.model Project SearchProjectsResponse)
           (com.google.api.services.iam.v1 IamScopes)
           (com.google.auth.http HttpCredentialsAdapter)
           (com.google.auth.oauth2 GoogleCredentials)))

(def page-size (Integer/parseInt (get-env "QUERY_PAGE_SIZE" "1000")))

(defn- cloud-resource-manager-service []
  (let [credential (.createScoped (GoogleCredentials/getApplicationDefault) [IamScopes/CLOUD_PLATFORM])
        builder (new CloudResourceManager$Builder
                     (GoogleNetHttpTransport/newTrustedTransport)
                     (GsonFactory/getDefaultInstance)
                     (new HttpCredentialsAdapter credential))
        svc (.. builder (setApplicationName "project-listing") build)
        ;  (.build  (.setApplicationName   builder "project-listing"));;todo delete
        ]

    svc))

(defn- process-project [^String org-id ^Project proj]

  (let [org-of (fn [proj-id]
                 (let [org-line (-> (process (conj '[gcloud projects get-ancestors] proj-id))
                                    check :out slurp str/split-lines last)
                       org (first (str/split org-line #"\s+"))]
                   org))
        project-id (.getProjectId proj)
        parent (.getParent proj)
        lifecycle-state (.getState proj)
        org-pfx "organizations/"
        org-pth (str org-pfx org-id)]

    (if (= lifecycle-state "ACTIVE")
      (cond (= parent org-pth)
            project-id                                      ; in our org
            (not (str/starts-with? parent org-pfx))
            (cond (= org-id (org-of project-id))            ; in a folder
                  project-id                                ; in a folder in our org
                  (empty? org-id)
                  (throw (Exception. (str project-id " is in parent " parent " yet no org was found")))
                  :else
                  nil)                                      ;  in a folder, but not in our org
            :else                                           ;It is directly in an org, but not ours
            nil)
      nil)))

(defn- one-page-query [^CloudResourceManager svc
                       ^String filter
                       ^String org-id
                       ^String tok]

  (let [^SearchProjectsResponse searched (.. svc (projects) (search) (setQuery filter) (setPageToken tok) (setPageSize page-size) execute)
        tok (.getNextPageToken searched)
        projs (.getProjects searched)
        project-ids-with-nils (pmap (partial process-project org-id) projs)
        proj-ids-in-org (remove nil? project-ids-with-nils)]
    {:projects proj-ids-in-org :token tok}))

(defn filtered-projects-in-org
  ([^String filter ^String org-id]
   (let [paged-query-int
         (fn []                                             ;   params from closure
           (let [svc (cloud-resource-manager-service)]
             (loop [tok nil
                    accumulator []]
               (let [{projects-from-query :projects
                      tok-from-query      :token} (one-page-query svc filter org-id tok)
                     accumulated-projects (concat accumulator projects-from-query)]
                 (.println *err* (str "So far loaded " (count accumulated-projects) " projects"))
                 (if (nil? tok-from-query)
                   accumulated-projects
                   (recur tok-from-query accumulated-projects))))))]
     (sort (paged-query-int))))

  ([] (filtered-projects-in-org (get-env "QUERY_FILTER" "NOT projectId=sys-*") ; e.g. "NOT displayName=doitintl* AND NOT projectId=sys-*"
                                (get-env "ORG_ID" :required)))) ; e.g. "970193134296"


