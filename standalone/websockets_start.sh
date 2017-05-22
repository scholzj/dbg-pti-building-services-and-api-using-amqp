#!/usr/bin/env bash

docker run -d -p 5673:5673 --name wsgw scholzj/qpid-dispatch:0.8.0 websockify 0.0.0.0:5673 $(ipconfig getifaddr en0):5672
