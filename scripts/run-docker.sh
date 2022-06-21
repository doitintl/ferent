#!/usr/bin/env bash
set -u
if [[ $(basename $PWD)  != "scripts" ]]; then
  echo "run in scripts dir" && exit 1
fi

echo "ORG_ID in run-docker.sh is $ORG_ID"

pushd ..
mkdir -p ./resources
docker run  --env ORG_ID="$ORG_ID" -t ferent
popd ||exit