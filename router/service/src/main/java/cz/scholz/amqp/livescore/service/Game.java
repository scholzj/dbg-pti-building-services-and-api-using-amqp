package cz.scholz.amqp.livescore.service;

/**
 * Created by jakub on 20/05/2017.
 */
public class Game {
    public final String homeTeam;
    public final String awayTeam;
    public Integer homeTeamGoals = 0;
    public Integer awayTeamGoals = 0;

    public Game(String homeTeam, String awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    public Game(String homeTeam, String awayTeam, int homeTeamGoals, int awayTeamGoals) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeTeamGoals = homeTeamGoals;
        this.awayTeamGoals = awayTeamGoals;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public int getHomeTeamGoals() {
        return homeTeamGoals;
    }

    public Game setHomeTeamGoals(int homeTeamGoals) {
        this.homeTeamGoals = homeTeamGoals;
        return this;
    }

    public int getAwayTeamGoals() {
        return awayTeamGoals;
    }

    public Game setAwayTeamGoals(int awayTeamGoals) {
        this.awayTeamGoals = awayTeamGoals;
        return this;
    }

    public Game setScore(int homeTeamGoals, int awayTeamGoals) {
        setHomeTeamGoals(homeTeamGoals);
        setAwayTeamGoals(awayTeamGoals);
        return this;
    }

    public String toString()
    {
        return String.format("Game: %s : %s, Score: %d : %d, Start time: %s, Game time: %s", homeTeam, awayTeam, homeTeamGoals, awayTeamGoals);
    }
}
