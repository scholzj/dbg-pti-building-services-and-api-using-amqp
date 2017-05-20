#!/usr/bin/env bash

docker run -d -p 5672:5672 --name router scholzj/building-services-and-apis-using-amqp:latest
