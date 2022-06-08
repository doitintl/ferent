#!/usr/bin/env bash
pushd ..
mkdir -p ./resources
docker run --mount type=bind,source="$(pwd)"/resources,target=/usr/src/app/resources -t ferent scan,calculate
popd ||exit