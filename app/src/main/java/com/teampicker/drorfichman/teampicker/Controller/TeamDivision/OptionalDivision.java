package com.teampicker.drorfichman.teampicker.Controller.TeamDivision;

import android.content.Context;

import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.Collaboration;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.CollaborationHelper;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.TeamPrediction;
import com.teampicker.drorfichman.teampicker.Data.BuilderPlayerCollaborationStatistics;
import com.teampicker.drorfichman.teampicker.Data.TeamData;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;

/**
 * Represents a potential team division for optimization.
 * Uses TeamPrediction.calculateBalanceScore for consistent calculations.
 */
public class OptionalDivision {
    public TeamData players1 = new TeamData();
    public TeamData players2 = new TeamData();

    /**
     * Get 2 teams diff of grade sum
     */
    public int getGradeDiff() {
        return Math.abs(players1.getSum() - players2.getSum());
    }

    /**
     * Calculate balance score for this division.
     * Lower score = more balanced teams.
     * <p>
     * Uses TeamPrediction.calculateBalanceScore as the single source of truth
     * for team balance calculation.
     *
     * @param ctx Context for accessing settings
     * @param params Collaboration statistics parameters
     * @return Balance score (0 = perfectly balanced, higher = more imbalanced)
     */
    public int getBalanceScore(Context ctx, BuilderPlayerCollaborationStatistics params) {
        // Get collaboration data for chemistry calculation
        Collaboration collaborationData = CollaborationHelper.getCollaborationData(
                ctx, players1.players, players2.players, params);

        // Calculate chemistry win rates
        int team1Chemistry = collaborationData.getCollaborationWinRate(players1.players);
        int team2Chemistry = collaborationData.getCollaborationWinRate(players2.players);

        // Calculate historical win rates and grade sums
        int team1WinRate = players1.getWinRate();
        int team2WinRate = players2.getWinRate();
        int team1GradeSum = players1.getSum();
        int team2GradeSum = players2.getSum();

        // Get weights from settings
        DivisionWeight weights = SettingsHelper.getDivisionWeight(ctx);

        // Use TeamPrediction for consistent calculation
        return TeamPrediction.calculateBalanceScore(
                team1Chemistry, team2Chemistry,
                team1WinRate, team2WinRate,
                team1GradeSum, team2GradeSum,
                weights);
    }
}
