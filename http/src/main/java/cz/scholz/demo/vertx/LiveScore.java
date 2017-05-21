package cz.scholz.demo.vertx;

import cz.scholz.demo.livescore.Game;
import cz.scholz.demo.livescore.InvalidGameException;
import cz.scholz.demo.livescore.LiveScoreService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Created by schojak on 10.1.17.
 */
public class LiveScore extends AbstractVerticle {
    final static private Logger LOG = LoggerFactory.getLogger(LiveScore.class);
    private LiveScoreService scoreService = new LiveScoreService();
    private Router router;
    private HttpServer server;

    @Override
    public void start(Future<Void> fut) {
        Integer httpPort = config().getJsonObject("http", new JsonObject()).getInteger("port", 8080);

        router = Router.router(vertx);

        router.route("/api/v1.0/*").handler(BodyHandler.create());
        router.get("/api/v1.0/score").handler(this::getScores);
        router.post("/api/v1.0/score").handler(this::addGame);
        router.put("/api/v1.0/score").handler(this::setScore);

        HttpServerOptions httpOptions = new HttpServerOptions();
        server = vertx.createHttpServer(httpOptions)
                .requestHandler(router::accept)
                .listen(httpPort);

        fut.complete();
    }

    public void setScore(RoutingContext routingContext)
    {
        LOG.info("Received PUT /scores request");

        JsonObject request = routingContext.getBodyAsJson();
        String homeTeam = request.getString("homeTeam", null);
        String awayTeam = request.getString("awayTeam", null);
        Integer homeTeamGoals = request.getInteger("homeTeamGoals", null);
        Integer awayTeamGoals = request.getInteger("awayTeamGoals", null);
        String gameTime = request.getString("gameTime", null);

        try {
            Game game = scoreService.setScore(homeTeam, awayTeam, homeTeamGoals, awayTeamGoals, gameTime);

            routingContext.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject(Json.encode(game)).encodePrettily());
        } catch (InvalidGameException e) {
            LOG.error("Failed to set new game score", e);

            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("error", e.getMessage()).encodePrettily());
        }
    }

    public void getScores(RoutingContext routingContext)
    {
        LOG.info("Received GET /scores request");

        JsonArray response = new JsonArray(Json.encode(scoreService.getScores()));

        LOG.info("Providing scores " + response);

        routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(response.encodePrettily());
    }

    public void addGame(RoutingContext routingContext)
    {
        LOG.info("Received POST /scores request");

        JsonObject request = routingContext.getBodyAsJson();
        String homeTeam = request.getString("homeTeam", null);
        String awayTeam = request.getString("awayTeam", null);
        String startTime = request.getString("startTime", null);

        try {
            LOG.info("Creating game " + request);
            Game game = scoreService.addGame(homeTeam, awayTeam, startTime);

            LOG.info("Game created " + game);

            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject(Json.encode(game)).encodePrettily());
        } catch (InvalidGameException e) {
            LOG.error("Failed to add new game", e);

            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("error", e.getMessage()).encodePrettily());
        }
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Shutting down");
        server.close();
    }
}
