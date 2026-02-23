package com.teampicker.drorfichman.teampicker.Data;

import android.content.Context;

import com.google.firebase.database.Exclude;
import com.teampicker.drorfichman.teampicker.tools.DateHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

/**
 * Created by drorfichman on 7/27/16.
 */
public class Game implements Serializable {

    public int gameId;
    public String dateString;
    public TeamEnum winningTeam;
    public int team1Score;
    public int team2Score;

    public ResultEnum playerResult;
    public ArrayList<String> team1 = new ArrayList<>();
    public ArrayList<String> team2 = new ArrayList<>();

    public Game() {}

    public void setTeams(ArrayList<Player> t1,ArrayList<Player> t2) {
        t1.forEach(p -> team1.add(p.mName));
        t2.forEach(p -> team2.add(p.mName));
    }

    @Exclude
    public int playerGrade;

    @Exclude
    public boolean playerIsMVP;

    @Exclude
    public boolean playerIsInjured;

    /** True when all filtered players were on the same (non-bench) team in this game. */
    @Exclude
    public boolean playersAllOnSameTeam;

    public Game(int gameId, String date, int score1, int score2) {
        this.gameId = gameId;
        this.dateString = date;
        this.team1Score = score1;
        this.team2Score = score2;
        this.winningTeam = TeamEnum.getResult(this.team1Score, this.team2Score);
    }

    @Exclude
    public String getScore() {
        return team1Score + " - " + team2Score;
    }

    public String getDisplayDate(Context ctx) {
        return DateHelper.getDisplayDate(ctx, this.dateString);
    }

    @Exclude
    public Date getDate() {
        return DateHelper.getDate(this.dateString);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return gameId == game.gameId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId);
    }
}
