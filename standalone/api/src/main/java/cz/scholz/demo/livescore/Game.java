package cz.scholz.demo.livescore;

/**
 * Created by schojak on 10.1.17.
 */
public class Game {
    private final String homeTeam;
    private final String awayTeam;
    private Integer homeTeamGoals = 0;
    private Integer awayTeamGoals = 0;

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
        return String.format("Game: %s : %s, Score: %d : %d", homeTeam, awayTeam, homeTeamGoals, awayTeamGoals);
    }
}
