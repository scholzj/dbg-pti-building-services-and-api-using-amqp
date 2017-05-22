#!/usr/bin/env bash

docker run -ti scholzj/qpid-cpp:1.36.0 qpid-receive -b $(ipconfig getifaddr en0):5672 --connection-options "{protocol: amqp1.0}" -a "'/liveScore'" -f
