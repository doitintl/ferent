(ns ferent.query-projects
  (:require [babashka.process :refer [check process]]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [com.climate.claypoole :as cp]
            [ferent.utils :refer [paginated-query thread-count]])

  (:import (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
           (com.google.api.client.json.gson GsonFactory)
           (com.google.api.services.cloudresourcemanager.v3 CloudResourceManager CloudResourceManager$Builder)
           (com.google.api.services.cloudresourcemanager.v3.model Project SearchProjectsResponse)
           (com.google.api.services.iam.v1 IamScopes)
           (com.google.auth.http HttpCredentialsAdapter)
           (com.google.auth.oauth2 GoogleCredentials)))

(defn- cloud-resource-manager-service []
  (let [credential (.createScoped (GoogleCredentials/getApplicationDefault) [IamScopes/CLOUD_PLATFORM])
        builder (new CloudResourceManager$Builder
                     (GoogleNetHttpTransport/newTrustedTransport)
                     (GsonFactory/getDefaultInstance)
                     (new HttpCredentialsAdapter credential))
        ;todo reduce use of let throughput
        svc (.. builder (setApplicationName "project-listing") build)]
    svc))

(defn- if-proj-in-org [^String org-id ^Project proj]
  ;; todo change the let sequence to  a function returning a map
  (let [org-of (fn [proj-id]                                ;to make this a non anonymous top-level fn
                 (let [org-line (-> (process (conj '[gcloud projects get-ancestors] proj-id))
                                    check :out slurp str/split-lines last)
                       org (first (str/split org-line #"\s+"))]
                   org))
        project-id (.getProjectId proj)
        parent (.getParent proj)
        lifecycle-state (.getState proj)
        org-pfx "organizations/"
        org-pth (str org-pfx org-id)]
    (when  (some? parent)
      ; If (some? parent) is false, then you have access to the proj but not parents;
      ; so, assuming permissions  that allow us to run the rest of this tool,
      ; this proj is not in your org.
      ; the when will return nil

      (if (= lifecycle-state "ACTIVE")
        (cond (= parent org-pth)
              project-id                                    ;Project is in our org
              (not (str/starts-with? parent org-pfx))
              (cond (= org-id (org-of project-id))          ; Project is in a folder
                    project-id                              ; Project is in a folder in our org
                    (empty? org-id)
                    (throw (Exception. (str project-id " is in parent " parent " yet no org was found")))
                    :else
                    nil)                                    ; Project is in a folder, but not in our org
              :else                                         ;Project is in an org, but not ours
              nil)
        nil))))

(defn- one-page-query [^CloudResourceManager svc
                       ^String filter
                       ^String org-id
                       ^String tok]

  (let [^SearchProjectsResponse searched (..
                                          svc (projects) (search)
                                          (setQuery filter) (setPageToken tok)
                                          execute)
        tok (.getNextPageToken searched)
        projs (.getProjects searched)
        project-ids-with-nils (cp/pmap (cp/threadpool thread-count) (partial if-proj-in-org org-id) projs)
        proj-ids-in-org (remove nil? project-ids-with-nils)]
    (.println *err* (str "Loaded " (count projs) " projects from one query page of which " (count proj-ids-in-org) " in org"))
    {:projects proj-ids-in-org :token tok}))

(defn filtered-projects-in-org [filter org-id]
  (let [svc (cloud-resource-manager-service)]
    (paginated-query (fn [tok] (rename-keys (one-page-query svc filter org-id tok)
                                            {:projects :items})))))

