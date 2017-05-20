package cz.scholz.amqp.livescore.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

/**
 * Created by jakub on 20/05/2017.
 */
public class AddGameListener implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddGameListener.class);
    private final LiveScoreService liveScore;
    private final Session session;
    private final Gson gson;

    public AddGameListener(LiveScoreService liveScore, Session session) {
        this.liveScore = liveScore;
        this.session = session;

        GsonBuilder builder = new GsonBuilder();
        this.gson = builder.create();
    }

    @Override
    public void onMessage(Message msg) {
        LOGGER.info("Received /addGame request");

        int statusCode = 400;
        String error = "Failed to set score";

        if (msg instanceof TextMessage) {
            try {
                Game game = gson.fromJson(((TextMessage) msg).getText(), Game.class);
                liveScore.addGame(game.getHomeTeam(), game.getAwayTeam());
                statusCode = 200;
            } catch (InvalidGameException e) {
                LOGGER.error("Failed to add game", e);
                error = "Failed to add game: " + e.getMessage();
            } catch (JMSException e) {
                LOGGER.error("Failed to add game - JSON decoding failed", e);
                error = "Failed to add game - JSON decoding failed";
            }
        }
        else {
            statusCode = 400;
            error = "Failed to add game - unknown message type " + msg.getClass().getCanonicalName();
            LOGGER.error("Failed to add game - unknown message type " + msg.getClass().getCanonicalName());
        }

        try {
            if (msg.getJMSReplyTo() != null) {
                MessageProducer responder = session.createProducer(msg.getJMSReplyTo());
                TextMessage response = session.createTextMessage();
                response.setIntProperty("status", statusCode);
                if (statusCode != 200) {
                    response.setText(gson.toJson(new ErrorMessage("error")));
                }
                responder.send(response);
            }
        } catch (JMSException e) {
            LOGGER.error("Failed to get JMS Reply To from /addGame request", e);
        }

        try {
            msg.acknowledge();
        } catch (JMSException e) {
            LOGGER.error("Failed to acknowledge /addGame request", e);
        }
    }
}
