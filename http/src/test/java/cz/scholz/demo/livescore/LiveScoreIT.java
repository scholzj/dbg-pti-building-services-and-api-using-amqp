package cz.scholz.demo.livescore;

import cz.scholz.demo.vertx.LiveScore;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by jakub on 15/01/2017.
 */
@RunWith(VertxUnitRunner.class)
public class LiveScoreIT {
    final static private Logger LOG = LoggerFactory.getLogger(LiveScoreIT.class);
    private static Vertx vertx;
    private static int httpPort;
    private static JsonObject config = new JsonObject();

    @BeforeClass
    public static void setUp(TestContext context) {
        vertx = Vertx.vertx();
        httpPort = Integer.getInteger("http.port", 8080);
        config.put("http", new JsonObject().put("port", httpPort));
    }

    private void deployVerticle(TestContext context)
    {
        final Async asyncStart = context.async();

        LOG.info("Starting verticle with config " + config);

        vertx.deployVerticle(LiveScore.class.getName(), new DeploymentOptions().setConfig(config), res -> {
            if (res.succeeded()) {
                asyncStart.complete();
            }
            else
            {
                context.fail(res.cause());
            }
        });

        asyncStart.awaitSuccess();
    }

    @Test
    public void testAddGame(TestContext context)
    {
        final Async asyncAddGame = context.async();

        deployVerticle(context);

        JsonObject payload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("startTime", "21th January 2017, 16:00");

        vertx.createHttpClient().post(httpPort, "localhost", "/api/v1.0/score", res -> {
            context.assertEquals(201, res.statusCode());
            res.bodyHandler(body -> {
                JsonObject bd = body.toJsonObject();
                context.assertEquals("Aston Villa", bd.getString("homeTeam"));
                context.assertEquals("Preston North End", bd.getString("awayTeam"));
                context.assertEquals("21th January 2017, 16:00", bd.getString("startTime"));
                context.assertEquals(0, bd.getInteger("homeTeamGoals"));
                context.assertEquals(0, bd.getInteger("awayTeamGoals"));
                context.assertEquals("0", bd.getString("gameTime"));
                asyncAddGame.complete();
            });
        }).end(Json.encodePrettily(payload));

        asyncAddGame.awaitSuccess(10000);
    }

    @Test
    public void testSetScore(TestContext context)
    {
        final Async asyncAddGame = context.async();
        final Async asyncSetScores = context.async();

        deployVerticle(context);

        JsonObject addGamePayload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("startTime", "21th January 2017, 16:00");
        JsonObject setScorePayload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("gameTime", "HT").put("homeTeamGoals", 1).put("awayTeamGoals", 0);

        vertx.createHttpClient().post(httpPort, "localhost", "/api/v1.0/score", res -> {
            context.assertEquals(201, res.statusCode());
            res.bodyHandler(body -> {
                JsonObject bd = body.toJsonObject();
                context.assertEquals("Aston Villa", bd.getString("homeTeam"));
                context.assertEquals("Preston North End", bd.getString("awayTeam"));
                context.assertEquals("21th January 2017, 16:00", bd.getString("startTime"));
                context.assertEquals(0, bd.getInteger("homeTeamGoals"));
                context.assertEquals(0, bd.getInteger("awayTeamGoals"));
                context.assertEquals("0", bd.getString("gameTime"));
                asyncAddGame.complete();
            });
        }).end(Json.encodePrettily(addGamePayload));

        asyncAddGame.awaitSuccess(10000);

        vertx.createHttpClient().put(httpPort, "localhost", "/api/v1.0/score", res -> {
            context.assertEquals(200, res.statusCode());
            res.bodyHandler(body -> {
                JsonObject bd = body.toJsonObject();
                context.assertEquals("Aston Villa", bd.getString("homeTeam"));
                context.assertEquals("Preston North End", bd.getString("awayTeam"));
                context.assertEquals("21th January 2017, 16:00", bd.getString("startTime"));
                context.assertEquals(1, bd.getInteger("homeTeamGoals"));
                context.assertEquals(0, bd.getInteger("awayTeamGoals"));
                context.assertEquals("HT", bd.getString("gameTime"));
                asyncSetScores.complete();
            });
        }).end(Json.encodePrettily(setScorePayload));

        asyncSetScores.awaitSuccess(10000);
    }

    @Test
    public void testInvalidSetScore(TestContext context)
    {
        final Async asyncAddGame = context.async();
        final Async asyncSetScores = context.async();

        deployVerticle(context);

        JsonObject addGamePayload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("startTime", "21th January 2017, 16:00");
        JsonObject setScorePayload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("gameTime", "HT").put("homeTeamGoals", -1).put("awayTeamGoals", 0);

        vertx.createHttpClient().post(httpPort, "localhost", "/api/v1.0/score", res -> {
            context.assertEquals(201, res.statusCode());
            res.bodyHandler(body -> {
                JsonObject bd = body.toJsonObject();
                context.assertEquals("Aston Villa", bd.getString("homeTeam"));
                context.assertEquals("Preston North End", bd.getString("awayTeam"));
                context.assertEquals("21th January 2017, 16:00", bd.getString("startTime"));
                context.assertEquals(0, bd.getInteger("homeTeamGoals"));
                context.assertEquals(0, bd.getInteger("awayTeamGoals"));
                context.assertEquals("0", bd.getString("gameTime"));
                asyncAddGame.complete();
            });
        }).end(Json.encodePrettily(addGamePayload));

        asyncAddGame.awaitSuccess(10000);

        vertx.createHttpClient().put(httpPort, "localhost", "/api/v1.0/score", res -> {
            context.assertEquals(400, res.statusCode());
            res.bodyHandler(body -> {
                JsonObject bd = body.toJsonObject();
                context.assertTrue(bd.containsKey("error"));
                asyncSetScores.complete();
            });
        }).end(Json.encodePrettily(setScorePayload));

        asyncSetScores.awaitSuccess(10000);
    }

    @Test
    public void testGetScore(TestContext context)
    {
        final Async asyncAddGame = context.async();
        final Async asyncGetScores = context.async();

        deployVerticle(context);

        JsonObject payload = new JsonObject().put("homeTeam", "Aston Villa").put("awayTeam", "Preston North End").put("startTime", "21th January 2017, 16:00");

        vertx.createHttpClient().post(httpPort, "localhost", "/api/v1.0/score", res -> {
            context.assertEquals(201, res.statusCode());
            res.bodyHandler(body -> {
                JsonObject bd = body.toJsonObject();
                context.assertEquals("Aston Villa", bd.getString("homeTeam"));
                context.assertEquals("Preston North End", bd.getString("awayTeam"));
                context.assertEquals("21th January 2017, 16:00", bd.getString("startTime"));
                context.assertEquals(0, bd.getInteger("homeTeamGoals"));
                context.assertEquals(0, bd.getInteger("awayTeamGoals"));
                context.assertEquals("0", bd.getString("gameTime"));
                asyncAddGame.complete();
            });
        }).end(Json.encodePrettily(payload));

        asyncAddGame.awaitSuccess(10000);

        vertx.createHttpClient().getNow(httpPort, "localhost", "/api/v1.0/score", res -> {
            context.assertEquals(200, res.statusCode());
            res.bodyHandler(body -> {
                JsonObject bd = body.toJsonArray().getJsonObject(0);
                context.assertEquals("Aston Villa", bd.getString("homeTeam"));
                context.assertEquals("Preston North End", bd.getString("awayTeam"));
                context.assertEquals("21th January 2017, 16:00", bd.getString("startTime"));
                context.assertEquals(0, bd.getInteger("homeTeamGoals"));
                context.assertEquals(0, bd.getInteger("awayTeamGoals"));
                context.assertEquals("0", bd.getString("gameTime"));
                asyncGetScores.complete();
            });
        });

        asyncGetScores.awaitSuccess(10000);
    }

    @After
    public void cleanup(TestContext context)
    {
        vertx.deploymentIDs().forEach(id -> {
            vertx.undeploy(id, context.asyncAssertSuccess());
        });
    }

    @AfterClass
    public static void tearDown(TestContext context) {
        LiveScoreIT.vertx.close(context.asyncAssertSuccess());
    }
}
