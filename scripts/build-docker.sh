#!/usr/bin/env bash
pushd ..
docker build -t ferent .
popd ||exit