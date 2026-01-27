package com.teampicker.drorfichman.teampicker.Controller.TeamDivision;

import android.content.Context;
import android.util.Log;

import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Divides players into 3 balanced teams based on their grades
 */
public class DivideByGrade3Teams implements IDivider3Teams {

    public void divide(Context ctx, @NonNull ArrayList<Player> comingPlayers,
                       @NonNull List<Player> team1,
                       @NonNull List<Player> team2,
                       @NonNull List<Player> team3,
                       int divideAttemptsCount,
                       TeamDivision.onTaskInProgress update) {

        // Storage for the best configuration found
        List<Player> bestT1 = new ArrayList<>();
        List<Player> bestT2 = new ArrayList<>();
        List<Player> bestT3 = new ArrayList<>();
        double lowestDiff = Double.MAX_VALUE;

        // Working copy to shuffle
        ArrayList<Player> shutteredList = new ArrayList<>(comingPlayers);

        for (int i = 0; i < divideAttemptsCount; i++) {
            Collections.shuffle(shutteredList);

            // Temp lists for this iteration
            List<Player> currentT1 = new ArrayList<>();
            List<Player> currentT2 = new ArrayList<>();
            List<Player> currentT3 = new ArrayList<>();

            // Round-robin distribution into 3 teams
            for (int p = 0; p < shutteredList.size(); p++) {
                int target = p % 3;
                if (target == 0) currentT1.add(shutteredList.get(p));
                else if (target == 1) currentT2.add(shutteredList.get(p));
                else currentT3.add(shutteredList.get(p));
            }

            // Calculate imbalance (Max Average - Min Average)
            double diff = calculateThreeTeamImbalance(currentT1, currentT2, currentT3);

            if (diff < lowestDiff) {
                lowestDiff = diff;
                bestT1 = new ArrayList<>(currentT1);
                bestT2 = new ArrayList<>(currentT2);
                bestT3 = new ArrayList<>(currentT3);
            }

            // Trigger progress callback
            if (update != null) {
                update.update(divideAttemptsCount - i, String.valueOf(lowestDiff));
            }
        }

        // Apply the best results to the provided references
        team1.clear(); team1.addAll(bestT1);
        team2.clear(); team2.addAll(bestT2);
        team3.clear(); team3.addAll(bestT3);
    }

    private double calculateThreeTeamImbalance(List<Player> t1, List<Player> t2, List<Player> t3) {
        double avg1 = getAverage(t1);
        double avg2 = getAverage(t2);
        double avg3 = getAverage(t3);

        double max = Math.max(avg1, Math.max(avg2, avg3));
        double min = Math.min(avg1, Math.min(avg2, avg3));

        return max - min;
    }

    private double getAverage(List<Player> players) {
        if (players.isEmpty()) return 0;
        double sum = 0;
        for (Player p : players) sum += p.grade();
        return sum / players.size();
    }
}

