#!/usr/bin/env bash
set -x

[ ! -d    resources ] &&  echo "Run in base dir" &&
[ -z "$ORG_ID" ]  && echo "Must define $ORG_ID" && exit 1

if [ -z "$EXCLUDE" ]; then
   EXCLUDE="^$"
else
  echo "Exclude these: $EXCLUDE"
fi

[ -z $REQUIRE ] || echo "only process this pattern: $REQUIRE"

for project_id in $(gcloud projects list --format='value(project_id)' | \
      egrep $REQUIRE | \
      egrep -v $EXCLUDE ); do
    org_id=$(gcloud projects get-ancestors $project_id | grep organization | cut -f1 -d' ')
    if [ "$org_id" -eq "$ORG_ID" ]; then
        # Get Service Accounts to which the proj grants permissions
       for sa in $( gcloud projects get-iam-policy $project_id --format json |jq  -r '.bindings[].members[]' | grep serviceAccount | sed  "s/serviceAccount\:\(.*\)/\1/"  ); do
          echo "$project_id,$sa" >> ./resources/permissions_granted_by_project_temp.csv
       done
       # Get Service Accounts that are *in* the proj
        for sa in $(gcloud iam service-accounts list --project $project_id --format="table[no-heading](email)" ); do
          echo  "$project_id,$sa"  >>  ./resources/sa_in_project_temp.csv
        done

    fi
done
sort -u   ./resources/permissions_granted_by_project.csv_tmp > ./resources/permissions_granted_by_project.csv
sort -u  ./resources/sa_in_project_temp.csv >> ./resources/sa_in_project.csv