package com.teampicker.drorfichman.teampicker.Data;

import android.content.Context;

import com.google.firebase.database.Exclude;
import com.teampicker.drorfichman.teampicker.tools.DateHelper;

import java.io.Serializable;
import java.util.Date;

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

    public Game() {}

    @Exclude
    public int playerGrade;

    public Game(int gameId, String date, int score1, int score2) {
        this.gameId = gameId;
        this.dateString = date;
        this.team1Score = score1;
        this.team2Score = score2;
        this.winningTeam = TeamEnum.getResult(this.team1Score, this.team2Score);
    }

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
}
