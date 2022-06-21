#!/usr/bin/env bash

if [[ $(basename $PWD)  != "scripts" ]]; then
  echo "run in scripts dir" && exit 1
fi

./build-docker.sh
./run-docker.sh