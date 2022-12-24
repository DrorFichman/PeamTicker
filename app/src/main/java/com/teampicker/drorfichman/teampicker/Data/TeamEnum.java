package com.teampicker.drorfichman.teampicker.Data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.DrawableRes;

import com.teampicker.drorfichman.teampicker.tools.ColorHelper;

/**
 * Created by drorfichman on 10/3/16.
 */
public enum TeamEnum {
    Team1(0),
    Team2(1),
    Tie(2), // TODO should move reference to result enum?
    Bench(3);

    private int teamColorIndex;

    TeamEnum(int teamColorIndex) {
        this.teamColorIndex = teamColorIndex;
    }

    public static TeamEnum getResultFromOrdinal(int res) {
        for (TeamEnum r : TeamEnum.values()) {
            if (r.teamColorIndex == res) { // TODO should reference ordinal value that's saved in DB (addPlayerGame)
                return r;
            }
        }
        return null;
    }

    @DrawableRes
    public int getDrawable(Context ctx) {
        if (teamColorIndex == 0 || teamColorIndex == 1 || teamColorIndex == 2) {
            int[] colors = ColorHelper.getTeamsIcons(ctx);
            return colors[teamColorIndex];
        } else {
            // TODO is this possible?
            Log.e("TeamEnum", "Get team color for " + teamColorIndex);
            return 0;
        }
    }

    public static TeamEnum getResult(int team1Score, int team2Score) {
        if (team1Score > team2Score) {
            return Team1;
        } else if (team2Score > team1Score) {
            return Team2;
        } else {
            return Tie;
        }
    }

    public static ResultEnum getTeam2Result(TeamEnum team) {
        if (team == Team2) {
            return ResultEnum.Win;
        } else if (team == Team1) {
            return ResultEnum.Lose;
        } else if (team == Tie) {
            return ResultEnum.Tie;
        } else {
            return ResultEnum.NA;
        }
    }

    public static ResultEnum getTeam1Result(TeamEnum team) {
        if (team == Team1) {
            return ResultEnum.Win;
        } else if (team == Team2) {
            return ResultEnum.Lose;
        } else if (team == Tie) {
            return ResultEnum.Tie;
        } else {
            return ResultEnum.NA;
        }
    }

    public static ResultEnum getTeamResultInGame(TeamEnum winningTeam, int playerTeam) {
        if (winningTeam == Tie) {
            return ResultEnum.Tie;
        } else if (winningTeam == Team1) {
            return playerTeam == Team1.ordinal() ? ResultEnum.Win : ResultEnum.Lose;
        } else if (winningTeam == Team2) {
            return playerTeam == Team2.ordinal() ? ResultEnum.Win : ResultEnum.Lose;
        }
        return ResultEnum.Missed;
    }
}
