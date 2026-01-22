package com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze;

import android.content.Context;

import com.teampicker.drorfichman.teampicker.Controller.TeamDivision.DivisionWeight;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.TeamData;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;

import java.util.List;

/**
 * Encapsulates team prediction calculation and stores all inputs used for the calculation.
 * <p>
 * Uses the same factors and weights as OptionalDivision.getBalanceScore:
 * - Chemistry: team collaboration win rate difference
 * - Win Rate: historical win rate difference (replaces stdDev)
 * - Grade: total grade difference
 */
public class TeamPrediction {

    public final int team1Probability;  // 0-100
    public final int team2Probability;  // 0-100

    // Inputs used for calculation (for display in stats table)
    public final int team1ChemistryWinRate;
    public final int team2ChemistryWinRate;
    public final int team1HistoricalWinRate;
    public final int team2HistoricalWinRate;
    public final int team1GradeSum;
    public final int team2GradeSum;

    private TeamPrediction(int team1Probability, int team2Probability,
                           int team1ChemistryWinRate, int team2ChemistryWinRate,
                           int team1HistoricalWinRate, int team2HistoricalWinRate,
                           int team1GradeSum, int team2GradeSum) {
        this.team1Probability = team1Probability;
        this.team2Probability = team2Probability;
        this.team1ChemistryWinRate = team1ChemistryWinRate;
        this.team2ChemistryWinRate = team2ChemistryWinRate;
        this.team1HistoricalWinRate = team1HistoricalWinRate;
        this.team2HistoricalWinRate = team2HistoricalWinRate;
        this.team1GradeSum = team1GradeSum;
        this.team2GradeSum = team2GradeSum;
    }

    /**
     * Calculate team prediction using weighted combination of chemistry, grades, and win rates.
     *
     * @param ctx Context for accessing settings
     * @param team1 List of players on team 1
     * @param team2 List of players on team 2
     * @return TeamPrediction with calculated probabilities and input values
     */
    public static TeamPrediction calculate(Context ctx, List<Player> team1, List<Player> team2) {
        // Get collaboration data for chemistry calculation
        Collaboration collaborationData = CollaborationHelper.getCollaborationData(ctx, team1, team2);

        // Calculate chemistry win rates
        int team1Chemistry = collaborationData.getCollaborationWinRate(team1);
        int team2Chemistry = collaborationData.getCollaborationWinRate(team2);

        // Calculate team data for grades and historical win rates
        int count = Math.min(team1.size(), team2.size());
        TeamData team1Data = new TeamData(team1, count);
        TeamData team2Data = new TeamData(team2, count);

        int team1WinRate = team1Data.getWinRate();
        int team2WinRate = team2Data.getWinRate();
        int team1GradeSum = team1Data.getSum();
        int team2GradeSum = team2Data.getSum();

        // Get weights from settings
        DivisionWeight weights = SettingsHelper.getDivisionWeight(ctx);

        // Calculate probability using testable method
        int team1Probability = calculateProbability(
                team1Chemistry, team2Chemistry,
                team1WinRate, team2WinRate,
                team1GradeSum, team2GradeSum,
                weights);

        int team2Probability = 100 - team1Probability;

        return new TeamPrediction(
                team1Probability, team2Probability,
                team1Chemistry, team2Chemistry,
                team1WinRate, team2WinRate,
                team1GradeSum, team2GradeSum
        );
    }

    /**
     * Calculate team1 win probability based on team statistics.
     * This method is static and context-free for unit testing.
     * 
     * Uses same logic as OptionalDivision but converts difference to probability:
     * - OptionalDivision measures imbalance (lower = more balanced)
     * - TeamPrediction measures win probability (50 = balanced)
     *
     * @param team1Chemistry Team 1 chemistry win rate (0-100, 0 if no data)
     * @param team2Chemistry Team 2 chemistry win rate (0-100, 0 if no data)
     * @param team1WinRate Team 1 historical win rate (0-100, 0 if no data)
     * @param team2WinRate Team 2 historical win rate (0-100, 0 if no data)
     * @param team1GradeSum Team 1 total grade sum
     * @param team2GradeSum Team 2 total grade sum
     * @param weights Division weights from settings
     * @return Team 1 win probability (20-80)
     */
    public static int calculateProbability(
            int team1Chemistry, int team2Chemistry,
            int team1WinRate, int team2WinRate,
            int team1GradeSum, int team2GradeSum,
            DivisionWeight weights) {

        // Chemistry advantage: difference between team chemistry win rates
        // If no data, treat as neutral (50%)
        int effective1Chemistry = team1Chemistry > 0 ? team1Chemistry : 50;
        int effective2Chemistry = team2Chemistry > 0 ? team2Chemistry : 50;
        double chemistryAdvantage = effective1Chemistry - effective2Chemistry;

        // Win rate advantage: difference between team win rates  
        // If no data, treat as neutral (50%)
        int effective1WinRate = team1WinRate > 0 ? team1WinRate : 50;
        int effective2WinRate = team2WinRate > 0 ? team2WinRate : 50;
        double winRateAdvantage = effective1WinRate - effective2WinRate;

        // Grade advantage: difference as percentage of total
        double gradeAdvantage = 0;
        int totalGrades = team1GradeSum + team2GradeSum;
        if (totalGrades > 0) {
            double team1GradePct = (double) team1GradeSum * 100 / totalGrades;
            gradeAdvantage = team1GradePct - 50;
        }

        // Weighted total advantage using same weights as OptionalDivision
        double weightedAdvantage =
                (chemistryAdvantage * weights.chemistry()) +
                (winRateAdvantage * weights.stdDev()) +
                (gradeAdvantage * weights.grade());

        // Convert advantage to probability (50 + advantage)
        int team1Probability = (int) Math.round(50 + weightedAdvantage);

        // Clamp to reasonable range (20-80)
        return Math.max(20, Math.min(80, team1Probability));
    }

    /**
     * Check if prediction has meaningful data (not just default 50-50)
     */
    public boolean hasData() {
        // Has data if any team has chemistry, win rate, or grades
        return team1ChemistryWinRate > 0 || team2ChemistryWinRate > 0 ||
               team1HistoricalWinRate > 0 || team2HistoricalWinRate > 0 ||
               team1GradeSum > 0 || team2GradeSum > 0;
    }

    /**
     * Calculate balance score for team division optimization.
     * Lower score = more balanced teams.
     * <p>
     * This is the single source of truth for team balance calculation,
     * used by both OptionalDivision (for team shuffling) and this class (for display).
     *
     * @return Balance score (0 = perfectly balanced, higher = more imbalanced)
     */
    public static int calculateBalanceScore(
            int team1Chemistry, int team2Chemistry,
            int team1WinRate, int team2WinRate,
            int team1GradeSum, int team2GradeSum,
            DivisionWeight weights) {
        
        int probability = calculateProbability(
                team1Chemistry, team2Chemistry,
                team1WinRate, team2WinRate,
                team1GradeSum, team2GradeSum,
                weights);
        
        // Balance score is how far from 50% (perfect balance)
        // 50% → 0 (perfect), 60% → 10, 70% → 20, etc.
        return Math.abs(probability - 50);
    }
}

