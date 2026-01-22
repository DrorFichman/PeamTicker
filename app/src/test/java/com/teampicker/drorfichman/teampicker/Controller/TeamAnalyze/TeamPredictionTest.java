package com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze;

import com.teampicker.drorfichman.teampicker.Controller.TeamDivision.DivisionWeight;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for TeamPrediction calculation logic.
 * 
 * These tests verify that TeamPrediction.calculateProbability produces
 * consistent results with the same weights used by OptionalDivision.getBalanceScore.
 * 
 * Key differences:
 * - OptionalDivision: measures team IMBALANCE (lower = more balanced)
 * - TeamPrediction: measures WIN PROBABILITY (50% = balanced)
 * 
 * When OptionalDivision returns 0 (perfectly balanced), TeamPrediction should return 50%.
 * When teams are unbalanced, the team with higher values should have higher probability.
 */
public class TeamPredictionTest {

    // Default weights: chemistry=40%, winRate(stdDev)=40%, grade=20%
    private static final DivisionWeight DEFAULT_WEIGHTS = new DivisionWeight(20, 40, 40);

    @Test
    public void equalTeams_returns50Percent() {
        // Equal chemistry, equal win rate, equal grades -> 50-50
        int result = TeamPrediction.calculateProbability(
                55, 55,  // chemistry
                50, 50,  // win rate
                70, 70,  // grades
                DEFAULT_WEIGHTS);
        
        assertEquals(50, result);
    }

    @Test
    public void noData_returns50Percent() {
        // No data for either team -> 50-50 (both default to 50%)
        int result = TeamPrediction.calculateProbability(
                0, 0,    // no chemistry data
                0, 0,    // no win rate data
                0, 0,    // no grade data
                DEFAULT_WEIGHTS);
        
        assertEquals(50, result);
    }

    @Test
    public void team1BetterChemistry_favorsTeam1() {
        // Team 1 has 60% chemistry, Team 2 has 40% -> Team 1 favored
        int result = TeamPrediction.calculateProbability(
                60, 40,  // chemistry: team1 better
                50, 50,  // win rate: equal
                50, 50,  // grades: equal
                DEFAULT_WEIGHTS);
        
        assertTrue(result > 50);
        // With 20% difference and 40% weight: advantage = 20 * 0.4 = 8
        // Expected: 50 + 8 = 58%
        assertEquals(58, result);
    }

    @Test
    public void team2BetterChemistry_favorsTeam2() {
        // Team 2 has 60% chemistry, Team 1 has 40% -> Team 2 favored
        int result = TeamPrediction.calculateProbability(
                40, 60,  // chemistry: team2 better
                50, 50,  // win rate: equal
                50, 50,  // grades: equal
                DEFAULT_WEIGHTS);
        
        assertTrue(result < 50);
        // With -20% difference and 40% weight: advantage = -20 * 0.4 = -8
        // Expected: 50 - 8 = 42%
        assertEquals(42, result);
    }

    @Test
    public void team1BetterWinRate_favorsTeam1() {
        // Team 1 has 60% win rate, Team 2 has 40% -> Team 1 favored
        int result = TeamPrediction.calculateProbability(
                50, 50,  // chemistry: equal
                60, 40,  // win rate: team1 better
                50, 50,  // grades: equal
                DEFAULT_WEIGHTS);
        
        assertTrue(result > 50);
        // With 20% difference and 40% weight: advantage = 20 * 0.4 = 8
        assertEquals(58, result);
    }

    @Test
    public void team1BetterGrades_favorsTeam1() {
        // Team 1 has 70 grade sum, Team 2 has 30 -> Team 1 favored
        int result = TeamPrediction.calculateProbability(
                50, 50,  // chemistry: equal
                50, 50,  // win rate: equal
                70, 30,  // grades: team1 better (70% of total)
                DEFAULT_WEIGHTS);
        
        assertTrue(result > 50);
        // Grade advantage: (70/100)*100 - 50 = 20%
        // With 20% weight: 20 * 0.2 = 4
        assertEquals(54, result);
    }

    @Test
    public void combinedAdvantages_accumulate() {
        // Team 1 better in all categories
        int result = TeamPrediction.calculateProbability(
                60, 40,  // chemistry: +20 -> +8
                60, 40,  // win rate: +20 -> +8
                60, 40,  // grades: 60% -> +10% -> +2
                DEFAULT_WEIGHTS);
        
        assertTrue(result > 60);
        // Total: 8 + 8 + 2 = 18, so 50 + 18 = 68
        assertEquals(68, result);
    }

    @Test
    public void extremeAdvantage_clampedTo80() {
        // Team 1 much better in all categories
        int result = TeamPrediction.calculateProbability(
                90, 10,  // chemistry: +80 -> +32
                90, 10,  // win rate: +80 -> +32
                90, 10,  // grades: 90% -> +40% -> +8
                DEFAULT_WEIGHTS);
        
        // Total would be 32 + 32 + 8 = 72, so 50 + 72 = 122
        // But clamped to 80
        assertEquals(80, result);
    }

    @Test
    public void extremeDisadvantage_clampedTo20() {
        // Team 2 much better in all categories
        int result = TeamPrediction.calculateProbability(
                10, 90,  // chemistry: -80 -> -32
                10, 90,  // win rate: -80 -> -32
                10, 90,  // grades: 10% -> -40% -> -8
                DEFAULT_WEIGHTS);
        
        // Total would be -32 - 32 - 8 = -72, so 50 - 72 = -22
        // But clamped to 20
        assertEquals(20, result);
    }

    @Test
    public void partialData_usesNeutralForMissing() {
        // Only team1 has chemistry data (60%), team2 defaults to 50%
        int result = TeamPrediction.calculateProbability(
                60, 0,   // chemistry: 60 vs 50 (default)
                50, 50,  // win rate: equal
                50, 50,  // grades: equal
                DEFAULT_WEIGHTS);
        
        // Chemistry advantage: 60 - 50 = 10, * 0.4 = 4
        assertEquals(54, result);
    }

    @Test
    public void zeroTotalGrades_noGradeAdvantage() {
        // No grade data
        int result = TeamPrediction.calculateProbability(
                50, 50,  // chemistry: equal
                50, 50,  // win rate: equal
                0, 0,    // no grades
                DEFAULT_WEIGHTS);
        
        assertEquals(50, result);
    }

    @Test
    public void customWeights_affectCalculation() {
        // Custom weights: 50% chemistry, 30% winRate, 20% grade
        DivisionWeight customWeights = new DivisionWeight(20, 50, 30);
        
        int result = TeamPrediction.calculateProbability(
                60, 40,  // chemistry: +20 -> +10 (with 50% weight)
                50, 50,  // win rate: equal
                50, 50,  // grades: equal
                customWeights);
        
        // With 50% chemistry weight: 20 * 0.5 = 10
        assertEquals(60, result);
    }

    @Test
    public void symmetry_swappingTeamsInvertsResult() {
        // Team 1 better
        int result1 = TeamPrediction.calculateProbability(
                60, 40,
                55, 45,
                60, 40,
                DEFAULT_WEIGHTS);
        
        // Swap teams
        int result2 = TeamPrediction.calculateProbability(
                40, 60,
                45, 55,
                40, 60,
                DEFAULT_WEIGHTS);
        
        // Results should be symmetric (within clamping range)
        assertEquals(100 - result1, result2);
    }

    // ============ Balance Score Tests ============
    // These verify calculateBalanceScore used by OptionalDivision

    @Test
    public void balanceScore_equalTeams_returnsZero() {
        int score = TeamPrediction.calculateBalanceScore(
                50, 50,  // chemistry: equal
                50, 50,  // win rate: equal
                50, 50,  // grades: equal
                DEFAULT_WEIGHTS);
        
        assertEquals(0, score);
    }

    @Test
    public void balanceScore_slightAdvantage_returnsSmallScore() {
        // Team 1 has slight chemistry advantage: 60 vs 40 -> probability ~58%
        int score = TeamPrediction.calculateBalanceScore(
                60, 40,  // chemistry: +20 -> +8% probability
                50, 50,  // win rate: equal
                50, 50,  // grades: equal
                DEFAULT_WEIGHTS);
        
        // Probability is 58%, so balance score = |58 - 50| = 8
        assertEquals(8, score);
    }

    @Test
    public void balanceScore_symmetric() {
        // Team 1 better
        int score1 = TeamPrediction.calculateBalanceScore(
                60, 40, 55, 45, 60, 40, DEFAULT_WEIGHTS);
        
        // Team 2 better (swapped)
        int score2 = TeamPrediction.calculateBalanceScore(
                40, 60, 45, 55, 40, 60, DEFAULT_WEIGHTS);
        
        // Balance scores should be the same (just different favored team)
        assertEquals(score1, score2);
    }

    @Test
    public void balanceScore_clampedAtMax30() {
        // Extreme imbalance
        int score = TeamPrediction.calculateBalanceScore(
                90, 10,  // chemistry: +80
                90, 10,  // win rate: +80
                90, 10,  // grades: 90%
                DEFAULT_WEIGHTS);
        
        // Probability clamped to 80%, so balance score = |80 - 50| = 30
        assertEquals(30, score);
    }
}
