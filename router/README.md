# AMQP Router example

This example demonstrates how to create AMQP API using Apache Qpid Dispatch router.

## Build the docker image

Go to dispatch directory and run `build.sh` script.

## Run the Docker images

Use `./start.sh` and `./stop.sh` to start and stop the images.

## API server

Run the Java application in the `service` directory for the API server.

## WebSocket client

Open `ui/client.html` to see the Javascript based client using the Rhea library. Connect to ws://localhost:5673 toconnect to the websocket bridge.

## Example commands:

### Add new game

```bash
docker run --link router:router -ti scholzj/qpid-cpp:1.36.0 qpid-send -b admin/123456@router:5672 --connection-options "{protocol: amqp1.0}" -a "'/addGame'" --content-string '{"homeTeam": "Aston Villa", "awayTeam": "Birmingham City"}'
```

### Update score

```bash
docker run --link router:router -ti scholzj/qpid-cpp:1.36.0 qpid-send -b admin/123456@router:5672 --connection-options "{protocol: amqp1.0}" -a "'/setScore'" --content-string '{"homeTeam": "Aston Villa", "awayTeam": "Birmingham City", "homeTeamGoals": 1, "awayTeamGoals": 0}'
docker run --link router:router -ti scholzj/qpid-cpp:1.36.0 qpid-send -b admin/123456@router:5672 --connection-options "{protocol: amqp1.0}" -a "'/setScore'" --content-string '{"homeTeam": "Aston Villa", "awayTeam": "Birmingham City", "homeTeamGoals": 2, "awayTeamGoals": 0}'
docker run --link router:router -ti scholzj/qpid-cpp:1.36.0 qpid-send -b admin/123456@router:5672 --connection-options "{protocol: amqp1.0}" -a "'/setScore'" --content-string '{"homeTeam": "Aston Villa", "awayTeam": "Birmingham City", "homeTeamGoals": 3, "awayTeamGoals": 0}'
```

### Get all games

```bash
docker run --link router:router -ti scholzj/qpid-cpp:1.36.0 qpid-receive -b router:5672 --connection-options "{protocol: amqp1.0}" -a "'/getScore'"
```

### Receive broadcasts

```bash
docker run --link router:router -ti scholzj/qpid-cpp:1.36.0 qpid-receive -b router:5672 --connection-options "{protocol: amqp1.0}" -a "'/liveScore'" -f
```
