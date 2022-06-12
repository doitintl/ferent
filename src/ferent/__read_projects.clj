;(ns ferent.__read-projects
;
;  (:import (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
;           (com.google.api.client.json.gson GsonFactory)
;           (com.google.api.services.cloudresourcemanager.v3 CloudResourceManager$Builder)
;           (com.google.api.services.iam.v2beta IamScopes)
;           (com.google.auth.http HttpCredentialsAdapter)
;           (com.google.auth.oauth2 GoogleCredentials)
;           ))
;
;
;(defn create-cloud-resource-manager-service []
;  (let [
;        transport (GoogleNetHttpTransport/newTrustedTransport)
;        default-credentials (GoogleCredentials/getApplicationDefault)
;        scoped-credential (.createScoped default-credentials [IamScopes/CLOUD_PLATFORM])
;
;        request-initializer (new HttpCredentialsAdapter scoped-credential)
;        gson-factory (GsonFactory/getDefaultInstance)
;        builder (new CloudResourceManager$Builder
;                     transport gson-factory request-initializer)
;        service (.setApplicationName builder "ferent")
;        ] (.build service)))
;
;(defn list-projects []
;  (let [cloud-resource-manager-service (create-cloud-resource-manager-service)
;       list-request-projects (.projects cloud-resource-manager-service )
;        list-request-list (.list list-request-projects)
;       list-response (.execute list-request-list )]
;    list-response))
;(defn -main [  ]
;  (prn (list-projects)))
;
;
