package com.teampicker.drorfichman.teampicker.Controller.TeamDivision;

import android.content.Context;

import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Created by drorfichman on 9/16/16.
 */
public class TeamDivision {

    public interface onTaskInProgress {
        void update(int remaining, String score);
    }

    public enum DivisionStrategy {
        Grade(new DivideByGrade(), "grade"),
        Age(new DivideByAge(), "age"),
        Optimize(new DivideCollaboration(), "stats");

        IDivider divider;
        public final String text;

        DivisionStrategy(IDivider selected, String name) {
            divider = selected;
            text = name;
        }

        public static DivisionStrategy fromString(String name) {
            for (DivisionStrategy strategy : DivisionStrategy.values()) {
                if (strategy.text.equals(name)) {
                    return strategy;
                }
            }
            return null;
        }
    }

    public static void dividePlayers(Context ctx, @NonNull List<Player> comingPlayers,
                                     @NonNull List<Player> resultPlayers1,
                                     @NonNull List<Player> resultPlayers2,
                                     DivisionStrategy strategy,
                                     onTaskInProgress update) {

        resultPlayers1.clear();
        resultPlayers2.clear();

        ArrayList<Player> players = cloneList(comingPlayers);
        Collections.sort(players);

        int divideAttemptsCount = SettingsHelper.getDivideAttemptsCount(ctx);

        strategy.divider.divide(ctx, players, resultPlayers1, resultPlayers2, divideAttemptsCount, update);
    }

    public static ArrayList<Player> cloneList(List<Player> players) {
        ArrayList<Player> clone = new ArrayList<>(players.size());
        clone.addAll(players);
        return clone;
    }
}
