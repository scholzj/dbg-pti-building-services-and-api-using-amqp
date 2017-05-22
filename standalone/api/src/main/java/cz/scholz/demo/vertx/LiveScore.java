package cz.scholz.demo.vertx;

import cz.scholz.demo.livescore.Game;
import cz.scholz.demo.livescore.InvalidGameException;
import cz.scholz.demo.livescore.LiveScoreService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.impl.MessageImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by schojak on 10.1.17.
 */
public class LiveScore extends AbstractVerticle {
    final static private Logger LOG = LoggerFactory.getLogger(LiveScore.class);
    private LiveScoreService scoreService = new LiveScoreService();
    private AMQPServer server;

    @Override
    public void start(Future<Void> fut) {
        Integer amqpPort = config().getJsonObject("amqp", new JsonObject()).getInteger("port", 5672);
        server = new AMQPServer(vertx, amqpPort);
        server.handleRequests("/addGame", this::addGame);
        server.handleRequests("/setScore", this::setScore);
        server.handleRequests("/getScore", this::getScore);
        server.registerBroadcastPoint("/liveScore");

        fut.complete();
    }

    public void addGame(Message msg)
    {
        Section amqpBody = (AmqpValue)msg.getBody();
        JsonObject body = new JsonObject();

        if (amqpBody instanceof AmqpValue) {
            AmqpValue amqpValueBody = (AmqpValue) amqpBody;
            body = new JsonObject(amqpValueBody.getValue().toString());
        }
        else {
            LOG.error("Failed to decode message body of type " + amqpBody.getClass().getCanonicalName());
        }

        LOG.info("Handler received /addGame message: " + body.encode());

        String homeTeam = body.getString("homeTeam", null);
        String awayTeam = body.getString("awayTeam", null);

        try {
            LOG.info("Adding game");
            Game game = scoreService.addGame(homeTeam, awayTeam);

            broadcastUpdate(game);

            if(msg.getReplyTo() != null) {
                Message response = new MessageImpl();
                response.setBody(new AmqpValue(Json.encode(game)));
                Map<String, Object> ap = new HashMap<>();
                ap.put("status", 201);
                response.setApplicationProperties(new ApplicationProperties(ap));
                response.setAddress(msg.getReplyTo());

                server.respond(msg.getReplyTo(), response);
            }
        } catch (InvalidGameException e) {
            LOG.error("Failed to add new game", e);

            if(msg.getReplyTo() != null) {
                Message response = new MessageImpl();
                response.setBody(new AmqpValue(new JsonObject().put("error", e.getMessage()).encode()));
                Map<String, Object> ap = new HashMap<>();
                ap.put("status", 400);
                response.setApplicationProperties(new ApplicationProperties(ap));
                response.setAddress(msg.getReplyTo());

                server.respond(msg.getReplyTo(), response);
            }
        }
    }

    public void setScore(Message msg)
    {
        Section amqpBody = (AmqpValue)msg.getBody();
        JsonObject body = new JsonObject();

        if (amqpBody instanceof AmqpValue) {
            AmqpValue amqpValueBody = (AmqpValue) amqpBody;
            body = new JsonObject(amqpValueBody.getValue().toString());
        }
        else {
            LOG.error("Failed to decode message body of type " + amqpBody.getClass().getCanonicalName());
        }

        LOG.info("Handler received /setScore message: " + body.encode());

        String homeTeam = body.getString("homeTeam", null);
        String awayTeam = body.getString("awayTeam", null);
        Integer homeTeamGoals = body.getInteger("homeTeamGoals", null);
        Integer awayTeamGoals = body.getInteger("awayTeamGoals", null);

        try {
            LOG.info("Setting game score");
            Game game = scoreService.setScore(homeTeam, awayTeam, homeTeamGoals, awayTeamGoals);

            broadcastUpdate(game);

            if(msg.getReplyTo() != null) {
                Message response = new MessageImpl();
                response.setBody(new AmqpValue(Json.encode(game)));
                Map<String, Object> ap = new HashMap<>();
                ap.put("status", 200);
                response.setApplicationProperties(new ApplicationProperties(ap));
                response.setAddress(msg.getReplyTo());

                server.respond(msg.getReplyTo(), response);
            }
        } catch (InvalidGameException e) {
            LOG.error("Failed to set game score", e);

            if(msg.getReplyTo() != null) {
                Message response = new MessageImpl();
                response.setBody(new AmqpValue(new JsonObject().put("error", e.getMessage()).encode()));
                Map<String, Object> ap = new HashMap<>();
                ap.put("status", 400);
                response.setApplicationProperties(new ApplicationProperties(ap));
                response.setAddress(msg.getReplyTo());

                server.respond(msg.getReplyTo(), response);
            }
        }
    }

    public void getScore(Message msg)
    {
        LOG.info("Handler received /getScore message");
        // TODO: send the response

        if(msg.getReplyTo() != null) {
            Message response = new MessageImpl();
            response.setBody(new AmqpValue(new JsonArray(Json.encode(scoreService.getScores())).encode()));
            Map<String, Object> ap = new HashMap<>();
            ap.put("status", 200);
            response.setApplicationProperties(new ApplicationProperties(ap));
            response.setAddress(msg.getReplyTo());

            server.respond(msg.getReplyTo(), response);
        }
    }

    public void broadcastUpdate(Game game)
    {
        LOG.info("Broadcasting game update " + game);

        Message msg = new MessageImpl();
        msg.setBody(new AmqpValue(Json.encode(game)));
        msg.setAddress("/liveScore");

        server.broadcast("/liveScore", msg);

        LOG.info("Broadcast sent");
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Shutting down");
        // Nothing to do
    }
}
