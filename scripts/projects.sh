#!/usr/bin/env bash
set -x
set -u

MY_ORG=$1

[ -z "$MY_ORG" ]  && exit 1

for project_id in $(gcloud projects list --format='value(project_id)' | grep playground |grep -v doitintl |grep -v "sys-"  ); do
    org_id=$(gcloud projects get-ancestors $project_id | grep organization | cut -f1 -d' ')
    if [ $org_id -eq $MY_ORG ]; then
        # Get Service Accounts to which the proj grants permissions
       for sa in $( gcloud projects get-iam-policy $project_id --format json |jq  -r '.bindings[].members[]' | grep serviceAccount | sed  "s/serviceAccount\:\(.*\)/\1/"  ); do
          echo "$project_id,$sa" >> permissions_granted_by_project.csv
       done
       # Get Service Accounts that are *in* the proj
        for sa in $(gcloud iam service-accounts list --project $project_id --format="table[no-heading](email)" ); do
          echo  "$project_id,$sa"  >> sa_in_project.csv
        done

    fi
done
sort -u  permissions_granted_by_project.csv >>permissions_granted_by_project2.csv
sort -u sa_in_project.csv >>sa_in_project2.csv