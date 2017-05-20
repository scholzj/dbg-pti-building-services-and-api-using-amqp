package cz.scholz.amqp.livescore.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

/**
 * Created by jakub on 20/05/2017.
 */
public class GetScoreListener implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetScoreListener.class);
    private final LiveScoreService liveScore;
    private final Session session;
    private final Gson gson;

    public GetScoreListener(LiveScoreService liveScore, Session session) {
        this.liveScore = liveScore;
        this.session = session;

        GsonBuilder builder = new GsonBuilder();
        this.gson = builder.create();
    }

    @Override
    public void onMessage(Message msg) {
        LOGGER.info("Received /getScore request");

        try {
            if (msg.getJMSReplyTo() != null) {
                MessageProducer responder = session.createProducer(msg.getJMSReplyTo());
                responder.send(session.createTextMessage(gson.toJson(liveScore.getScores())));
            }
        } catch (JMSException e) {
            LOGGER.error("Failed to get JMS Reply To from /getScore request", e);
        }

        try {
            msg.acknowledge();
        } catch (JMSException e) {
            LOGGER.error("Failed to acknowledge /getScore request", e);
        }
    }
}
