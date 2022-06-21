#!/usr/bin/env bash


if [[ $(basename $PWD)  != "scripts" ]]; then
  echo "run in scripts dir" && exit 1
fi

pushd ..
docker build -t ferent .
popd ||exit