package com.teampicker.drorfichman.teampicker.Controller.TeamDivision;

import android.content.Context;

import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Helper class for dividing players into 3 teams
 */
public class TeamDivision3Teams {

    public enum DivisionStrategy3Teams {
        Grade(new DivideByGrade3Teams(), "grade");

        IDivider3Teams divider;
        public final String text;

        DivisionStrategy3Teams(IDivider3Teams selected, String name) {
            divider = selected;
            text = name;
        }

        public static DivisionStrategy3Teams fromString(String name) {
            for (DivisionStrategy3Teams strategy : DivisionStrategy3Teams.values()) {
                if (strategy.text.equals(name)) {
                    return strategy;
                }
            }
            return null;
        }
    }

    public static void dividePlayers(Context ctx, @NonNull List<Player> comingPlayers,
                                     @NonNull List<Player> resultTeam1,
                                     @NonNull List<Player> resultTeam2,
                                     @NonNull List<Player> resultTeam3,
                                     DivisionStrategy3Teams strategy,
                                     TeamDivision.onTaskInProgress update) {

        resultTeam1.clear();
        resultTeam2.clear();
        resultTeam3.clear();

        ArrayList<Player> players = cloneList(comingPlayers);
        Collections.sort(players);

        int divideAttemptsCount = SettingsHelper.getDivideAttemptsCount(ctx);

        strategy.divider.divide(ctx, players, resultTeam1, resultTeam2, resultTeam3, divideAttemptsCount, update);
    }

    public static ArrayList<Player> cloneList(@NonNull List<Player> comingPlayers) {
        ArrayList<Player> clone = new ArrayList<>(comingPlayers.size());
        clone.addAll(comingPlayers);
        return clone;
    }
}

