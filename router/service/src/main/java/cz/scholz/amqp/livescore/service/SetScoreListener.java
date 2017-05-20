package cz.scholz.amqp.livescore.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

/**
 * Created by jakub on 20/05/2017.
 */
public class SetScoreListener implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetScoreListener.class);
    private final LiveScoreService liveScore;
    private final Session session;
    private final Gson gson;

    public SetScoreListener(LiveScoreService liveScore, Session session) {
        this.liveScore = liveScore;
        this.session = session;

        GsonBuilder builder = new GsonBuilder();
        this.gson = builder.create();
    }

    @Override
    public void onMessage(Message msg) {
        LOGGER.info("Received /setScore request");

        int statusCode = 400;
        String error = "Failed to set score";

        if (msg instanceof TextMessage) {
            try {
                Game game = gson.fromJson(((TextMessage) msg).getText(), Game.class);
                liveScore.setScore(game.getHomeTeam(), game.getAwayTeam(), game.getHomeTeamGoals(), game.getAwayTeamGoals());
                statusCode = 200;
            } catch (InvalidGameException e) {
                LOGGER.error("Failed to set score", e);
                error = "Failed to set score: " + e.getMessage();
            } catch (JMSException e) {
                LOGGER.error("Failed to set score - JSON decoding failed", e);
                error = "Failed to set score - JSON decoding failed";
            }
        }
        else {
            statusCode = 400;
            error = "Failed to set score - unknown message type " + msg.getClass().getCanonicalName();
            LOGGER.error("Failed to set score - unknown message type " + msg.getClass().getCanonicalName());
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
            LOGGER.error("Failed to get JMS Reply To from /setScore request", e);
        }

        try {
            msg.acknowledge();
        } catch (JMSException e) {
            LOGGER.error("Failed to acknowledge /setScore request", e);
        }
    }
}
