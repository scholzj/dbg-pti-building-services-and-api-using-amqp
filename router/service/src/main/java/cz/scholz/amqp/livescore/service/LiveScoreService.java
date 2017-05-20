package cz.scholz.amqp.livescore.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jakub on 20/05/2017.
 */
public class LiveScoreService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveScoreService.class);

    private ConcurrentHashMap<String, Game> games = new ConcurrentHashMap();
    private final Gson gson;
    private Session liveScoreSession = null;
    private MessageProducer liveScoreProducer = null;

    public LiveScoreService() {
        GsonBuilder builder = new GsonBuilder();
        this.gson = builder.create();
    }

    public LiveScoreService setLiveScoreProducer(Session session, MessageProducer liveScoreProducer)
    {
        this.liveScoreProducer = liveScoreProducer;
        this.liveScoreSession = session;
        return this;
    }

    public List<Game> getScores() {
        return new LinkedList<Game>(games.values());
    }

    public Game getScore(String homeTeam, String awayTeam) throws InvalidGameException {
        if (!gameExists(homeTeam, awayTeam))
        {
            throw new InvalidGameException("Game between " + homeTeam + " and " + awayTeam + " was not found!");
        }
        else
        {
            return getGame(homeTeam, awayTeam);
        }
    }

    public Game addGame(String homeTeam, String awayTeam) throws InvalidGameException {
        if (homeTeam == null || awayTeam == null)
        {
            throw new InvalidGameException("Some of the mandatory fields (homeTeam or awayTeam) is invalid");
        }
        else if (!gameExists(homeTeam, awayTeam))
        {
            Game game = new Game(homeTeam, awayTeam);
            games.put(gameId(homeTeam, awayTeam), game);
            pushUpdate(game);
            return game;
        }
        else
        {
            throw new InvalidGameException("Game between " + homeTeam + " and " + awayTeam + " already exists!");
        }
    }

    public Game setScore(String homeTeam, String awayTeam, Integer homeTeamGoals, Integer awayTeamGoals) throws InvalidGameException {

        if (homeTeam == null || awayTeam == null || homeTeamGoals == null || awayTeamGoals == null)
        {
            throw new InvalidGameException("Some of the mandatory fields (homeTeam, awayTeam, homeTeamGoals, awayTeamGoals) is invalid");
        }
        else if (homeTeamGoals < 0 || awayTeamGoals < 0)
        {
            throw new InvalidGameException("The home and away team goals have to be => 0!");
        }
        else if (!gameExists(homeTeam, awayTeam))
        {
            throw new InvalidGameException("Game between " + homeTeam + " and " + awayTeam + " was not found! Maybe you forgot to create the game first?");
        }
        else
        {
            Game game = getGame(homeTeam, awayTeam).setScore(homeTeamGoals, awayTeamGoals);
            pushUpdate(game);
            return game;
        }
    }

    private String gameId(String homeTeam, String awayTeam)
    {
        return homeTeam + awayTeam;
    }

    private Boolean gameExists(String homeTeam, String awayTeam)
    {
        return games.containsKey(gameId(homeTeam, awayTeam));
    }

    private Game getGame(String homeTeam, String awayTeam)
    {
        return games.getOrDefault(gameId(homeTeam, awayTeam), null);
    }

    private void pushUpdate(Game game)
    {
        try {
            LOGGER.info("Broadcasting live score update");
            TextMessage broadcast = liveScoreSession.createTextMessage(gson.toJson(game));
            liveScoreProducer.send(broadcast);
            LOGGER.info("Broadcasting done");
        } catch (JMSException e) {
            LOGGER.error("Failed to broadcast live score update", e);
        }
    }
}
