#!/usr/bin/env bash

docker run --link router:router -ti scholzj/qpid-cpp:1.36.0 qpid-send -b admin/123456@router:5672 --connection-options "{protocol: amqp1.0}" -a "'/setScore'" --content-string '{"homeTeam": "Aston Villa", "awayTeam": "Birmingham City", "homeTeamGoals": 3, "awayTeamGoals": 0}'
