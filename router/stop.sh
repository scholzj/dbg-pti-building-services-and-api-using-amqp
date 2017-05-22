#!/usr/bin/env bash

docker stop router
docker stop wsgw
docker rm router
docker rm wsgw
