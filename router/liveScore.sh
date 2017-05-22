#!/usr/bin/env bash

docker run --link router:router -ti scholzj/qpid-cpp:1.36.0 qpid-receive -b router:5672 --connection-options "{protocol: amqp1.0}" -a "'/liveScore'" -f
