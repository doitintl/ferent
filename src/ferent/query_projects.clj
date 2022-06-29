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
  (let [builder (new CloudResourceManager$Builder
                     (GoogleNetHttpTransport/newTrustedTransport)
                     (GsonFactory/getDefaultInstance)
                     (new HttpCredentialsAdapter
                          (.createScoped (GoogleCredentials/getApplicationDefault) [IamScopes/CLOUD_PLATFORM])))]
    (.. builder (setApplicationName "project-listing") build)))

;todo use fewer let throughout
(defn- org-of [proj-id]
  (let [line-with-org (-> (process (conj '[gcloud projects get-ancestors] proj-id))
                          check :out slurp str/split-lines last)]
    (first (str/split line-with-org #"\s+"))))

(defn- project-details [proj]
  {:project-id      (.getProjectId proj)
   :parent          (.getParent proj)
   :lifecycle-state (.getState proj)})

(defn- if-proj-in-org [^String org-id ^Project proj]
  (let [{:keys [project-id parent lifecycle-state]} (project-details proj)
        org-pfx "organizations/"]

    (when (some? parent)
      ; About the above line (when.). If (some? parent) is false,
      ; then you have access to the proj but not the parents.
      ; So, assuming the permissions  that allow us to run the rest of this tool,
      ; this proj is not in your org. In such cases, `when` will return nil.
      (if (= lifecycle-state "ACTIVE")
        (cond (= parent (str org-pfx org-id))
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

(defn- one-page-query [^CloudResourceManager svc filter org-id pagination-token]

  (let [^SearchProjectsResponse search-response (..
                                                 svc (projects) (search)
                                                 (setQuery filter) (setPageToken pagination-token)
                                                 execute)
        proj-ids-in-org (remove nil? (cp/pmap (cp/threadpool thread-count) (partial if-proj-in-org org-id) (.getProjects search-response)))]
    (.println *err* (str "Loaded " (count (.getProjects search-response)) " projects from one query page of which " (count proj-ids-in-org) " in org"))

    {:projects proj-ids-in-org :token (.getNextPageToken search-response)}))

(defn filtered-projects-in-org [filter org-id]
  (let [svc (cloud-resource-manager-service)]
    (paginated-query
     (fn [tok] (rename-keys (one-page-query svc filter org-id tok) {:projects :items})))))

