#!/usr/bin/env sh

echo "ORG_ID in run.sh is $ORG_ID"
if [ -z "$ORG_ID" ]
then
     echo "ORG_ID environment variable is empty in docker container in run.sh; exiting" ; exit 1
fi

lein run