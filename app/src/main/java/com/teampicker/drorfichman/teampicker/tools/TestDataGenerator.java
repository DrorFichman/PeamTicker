package com.teampicker.drorfichman.teampicker.tools;

import android.content.Context;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generates sample players and games for admin chart testing; stored like any other local data.
 */
public class TestDataGenerator {

    private static final int GRADE_MIN = 45;
    private static final int GRADE_MAX = 90;
    /** Per-team size (both teams equal so wins/losses balance across all players). */
    private static final int TEAM_SIZE_MIN = 7;
    private static final int TEAM_SIZE_MAX = 9;
    private static final int SCORE_MIN = 1;
    private static final int SCORE_MAX = 8;
    private static final double ATTRIBUTE_CHANCE = 0.2;

    /**
     * Result of a successful generation.
     */
    public static class Result {
        public final int minGameId;
        public final int maxGameId;

        public Result(int minGameId, int maxGameId) {
            this.minGameId = minGameId;
            this.maxGameId = maxGameId;
        }
    }

    private final Random random = new Random();

    /**
     * Generate players and games. Call from background thread.
     *
     * @param ctx         Context
     * @param playerCount Number of players to create (e.g. 30)
     * @param gameCount   Number of games to create (e.g. 50)
     * @return Result with min/max game IDs, or null on failure
     */
    public Result generateAll(Context ctx, int playerCount, int gameCount) {
        ArrayList<Player> players = generatePlayers(ctx, playerCount);
        if (players.isEmpty()) {
            return null;
        }
        return generateGames(ctx, players, gameCount);
    }

    /**
     * Create disposable players with random grades and attributes.
     */
    public ArrayList<Player> generatePlayers(Context ctx, int count) {
        ArrayList<Player> players = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Player p = new Player("Test P" + i, GRADE_MIN + random.nextInt(GRADE_MAX - GRADE_MIN + 1));
            // Random attributes on ~20% of players
            if (random.nextDouble() < ATTRIBUTE_CHANCE) {
                int attr = random.nextInt(6);
                switch (attr) {
                    case 0: p.isGK = true; break;
                    case 1: p.isDefender = true; break;
                    case 2: p.isPlaymaker = true; break;
                    case 3: p.isUnbreakable = true; break;
                    case 4: p.isExtra = true; break;
                    case 5: p.isInjured = true; break;
                }
            }
            if (DbHelper.insertPlayer(ctx, p)) {
                players.add(p);
            }
        }
        return players;
    }

    /**
     * Create random games. Both teams have the same size each game so aggregate
     * success (wins - losses) sums to zero across all participating players.
     */
    public Result generateGames(Context ctx, List<Player> players, int gameCount) {
        if (players.size() < TEAM_SIZE_MIN * 2) {
            return null;
        }

        int nextGameId = DbHelper.getMaxGame(ctx) + 1;
        int minGameId = nextGameId;

        // Games spread over past weeks (~2 per week)
        long now = System.currentTimeMillis();
        long dayMs = 24 * 60 * 60 * 1000L;
        int weeksSpan = Math.max(1, gameCount / 2);

        for (int g = 0; g < gameCount; g++) {
            int gameId = nextGameId + g;

            // Date: spread over past weeks
            int dayOffset = random.nextInt(weeksSpan * 7);
            String dateString = DateHelper.getDate(now - dayOffset * dayMs);

            // Equal team sizes (7–9 each) so per-game success sum is always 0
            int maxHalf = Math.min(TEAM_SIZE_MAX, players.size() / 2);
            int half = TEAM_SIZE_MIN + random.nextInt(Math.max(1, maxHalf - TEAM_SIZE_MIN + 1));
            if (2 * half > players.size()) {
                half = players.size() / 2;
            }
            int team1Size = half;
            int team2Size = half;
            int playingCount = team1Size + team2Size;

            // Shuffle and assign
            ArrayList<Player> shuffled = new ArrayList<>(players);
            Collections.shuffle(shuffled, random);

            ArrayList<Player> team1 = new ArrayList<>(shuffled.subList(0, team1Size));
            ArrayList<Player> team2 = new ArrayList<>(shuffled.subList(team1Size, playingCount));
            ArrayList<Player> bench = new ArrayList<>();
            ArrayList<Player> missed = new ArrayList<>(shuffled.subList(playingCount, players.size()));

            // Random scores for chart variance (~40% T1 win, ~40% T2 win, ~20% tie)
            int score1 = SCORE_MIN + random.nextInt(SCORE_MAX - SCORE_MIN + 1);
            int score2 = SCORE_MIN + random.nextInt(SCORE_MAX - SCORE_MIN + 1);
            int outcome = random.nextInt(100);
            if (outcome < 40) {
                // Team1 wins
                if (score1 <= score2) score1 = score2 + 1;
            } else if (outcome < 80) {
                // Team2 wins
                if (score2 <= score1) score2 = score1 + 1;
            } else {
                // Tie
                score1 = score2;
            }

            Game game = new Game(gameId, dateString, score1, score2);
            DbHelper.saveTeams(ctx, team1, team2, bench, missed, null);
            DbHelper.insertGame(ctx, game, missed);
        }

        int maxGameId = nextGameId + gameCount - 1;
        return new Result(minGameId, maxGameId);
    }
}
