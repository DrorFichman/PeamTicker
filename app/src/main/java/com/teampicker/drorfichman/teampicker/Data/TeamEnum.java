package com.teampicker.drorfichman.teampicker.Data;

import android.content.Context;

import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.ColorHelper;

import androidx.annotation.DrawableRes;

/**
 * Created by drorfichman on 10/3/16.
 */
public enum TeamEnum {
    Team1(0, -1),
    Team2(1, -1),
    Tie(-1, R.drawable.circle_draw),
    Bench(-1, -1);

    private int drawable;
    private int teamColorIndex;

    TeamEnum(int drawableIndex, int drawableResource) {
        this.teamColorIndex = drawableIndex;
        drawable = drawableResource;
    }

    public static TeamEnum getResultFromOrdinal(int res) {
        for (TeamEnum r : TeamEnum.values()) {
            if (r.teamColorIndex == res) {
                return r;
            }
        }
        return null;
    }

    @DrawableRes
    public int getDrawable(Context ctx) {
        if (teamColorIndex == 0 || teamColorIndex == 1) {
            int[] colors = ColorHelper.getTeamsIcons(ctx);
            return colors[teamColorIndex];
        } else {
            return drawable;
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
