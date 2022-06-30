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


(defn org-of [proj-id]
  "Returns the org-id of the project with proj-id.
  We call this function only where the parent returned by the query is not
  already an org (so greatly reducing the numbers of calls to the Google Cloud
  command-line/API)."
  (let [line-with-org (-> (process (conj '[gcloud projects get-ancestors] proj-id))
                          check :out slurp str/split-lines last)]
    (when (str/ends-with? line-with-org "project")
      (throw (IllegalArgumentException.
               (str "Project "
                    proj-id
                    " has no parent info, and so org-of should not be called. "
                    "Probably it  is in another org to which you lack permissions."
                    )))
      )
    (assert (str/ends-with? line-with-org "organization") (str "Last line for ancestors of " proj-id " was " line-with-org))
    (first (str/split line-with-org #"\s+"))))

(defn- project-details [^Project proj]
  "Extract fields from object proj"
  {:project-id      (.getProjectId proj)
   :parent          (.getParent proj)
   :lifecycle-state (.getState proj)})

(defn- if-proj-in-org [org-id ^Project proj]
  "Returns the project-id of the proj if and only if the proj is in organization with org-id."
  (let [{:keys [project-id parent lifecycle-state]} (project-details proj)
        org-pfx "organizations/"]
    (when (some? parent)
      ; If there is no parent in the proj object (and so above when-statement is false),
      ; then you  lack permissions to learn   the parent, and we will assume that this proj is not in your org.
      ; (Noting that you *need* org permissions in your own org to run Ferent at all.)
      (if (= lifecycle-state "ACTIVE")
        (cond (= parent (str org-pfx org-id))
              project-id                                    ; Project is in our org
              (not (str/starts-with? parent org-pfx))
              (cond (= org-id (org-of project-id))          ; Project is in a folder
                    project-id                              ; Project is in a folder in our org
                    (empty? org-id)
                    (throw (Exception. (str project-id " is in parent " parent " yet no org was found")))
                    :else
                    nil)                                    ; Project is in a folder, but not in our org
              :else                                         ; Project is in an org, but not ours
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

(comment (org-of "sturdy-index-245812"))