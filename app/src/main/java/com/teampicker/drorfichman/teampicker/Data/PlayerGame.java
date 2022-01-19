package com.teampicker.drorfichman.teampicker.Data;

import java.io.Serializable;

public class PlayerGame implements Serializable {

    public String playerName;
    public int gameId;
    public String date;
    public int playerGrade;
    public int playerAge;
    public TeamEnum team;
    public ResultEnum result;
    public int didWin;
    public String attributes;

    public PlayerGame() {
    }

    public PlayerGame(int gameId, String playerName, int playerGrade, TeamEnum playerTeam, int playerAge, String attributes) {
        this.gameId = gameId;
        this.playerName = playerName;
        this.playerGrade = playerGrade;
        this.team = playerTeam;
        this.playerAge = playerAge;
        this.attributes = attributes;
    }
}
