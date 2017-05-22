#!/usr/bin/env bash

docker rm router
docker rm wsgw
docker run -d -p 5672:5672 --name router scholzj/building-services-and-apis-using-amqp:latest
docker run -d -p 5673:5673 --name wsgw --link router:router scholzj/qpid-dispatch:0.8.0 websockify 0.0.0.0:5673 router:5672
