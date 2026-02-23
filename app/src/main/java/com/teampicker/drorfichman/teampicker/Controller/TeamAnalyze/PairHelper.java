package com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze;

import android.content.Context;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.TeamEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes 2-player pair chemistry statistics.
 *
 * For each game, all C(n,2) combinations of players on the same team are enumerated.
 * Each pair is ranked by wins-minus-losses (2*wins - games) so volume matters:
 * 9W/1L outranks 3W/0L.
 */
public class PairHelper {

    public static final int MIN_GAMES_TOGETHER = 3;

    public static class PairStats {
        public final String playerA;
        public final String playerB;
        public final int gamesTogether;
        public final int wins;

        PairStats(String playerA, String playerB, int gamesTogether, int wins) {
            this.playerA = playerA;
            this.playerB = playerB;
            this.gamesTogether = gamesTogether;
            this.wins = wins;
        }

        public int getWinRate() {
            return gamesTogether > 0 ? (wins * 100 / gamesTogether) : 0;
        }

        public List<String> playerNames() {
            return Arrays.asList(playerA, playerB);
        }

        public String displayLabel() {
            return playerA + " & " + playerB;
        }
    }

    private static class MutableStats {
        int games = 0;
        int wins = 0;
    }

    /**
     * Returns all pairs sorted by wins-minus-losses, filtered by minimum games together.
     *
     * @param context   app context
     * @param gameCount number of recent games to consider, or -1 for all time
     */
    public static List<PairStats> computePairs(Context context, int gameCount) {
        ArrayList<Game> games = DbHelper.getGames(context, gameCount);

        Map<String, MutableStats> accumulator = new HashMap<>();

        for (Game game : games) {
            boolean teamWon1 = game.winningTeam == TeamEnum.Team1;
            boolean teamWon2 = game.winningTeam == TeamEnum.Team2;

            ArrayList<Player> team1 = DbHelper.getGameTeam(context, game.gameId, TeamEnum.Team1, 0);
            ArrayList<Player> team2 = DbHelper.getGameTeam(context, game.gameId, TeamEnum.Team2, 0);

            enumeratePairs(team1, teamWon1, accumulator);
            enumeratePairs(team2, teamWon2, accumulator);
        }

        List<PairStats> result = new ArrayList<>();
        for (Map.Entry<String, MutableStats> entry : accumulator.entrySet()) {
            MutableStats stats = entry.getValue();
            if (stats.games < MIN_GAMES_TOGETHER) continue;

            String[] names = entry.getKey().split("\\|");
            if (names.length != 2) continue;
            result.add(new PairStats(names[0], names[1], stats.games, stats.wins));
        }

        // Sort by wins-minus-losses (2*wins - games), break ties by win rate
        result.sort((a, b) -> {
            int scoreA = 2 * a.wins - a.gamesTogether;
            int scoreB = 2 * b.wins - b.gamesTogether;
            if (scoreB != scoreA) return Integer.compare(scoreB, scoreA);
            return Integer.compare(b.getWinRate(), a.getWinRate());
        });

        return new ArrayList<>(result.subList(0, Math.min(100, result.size())));
    }

    /** Generates all C(n,2) combinations from a team roster and updates the accumulator. */
    private static void enumeratePairs(List<Player> team, boolean teamWon,
                                       Map<String, MutableStats> accumulator) {
        int n = team.size();
        if (n < 2) return;

        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                String[] names = {team.get(i).mName, team.get(j).mName};
                Arrays.sort(names); // canonical order
                String key = names[0] + "|" + names[1];

                MutableStats stats = accumulator.computeIfAbsent(key, k -> new MutableStats());
                stats.games++;
                if (teamWon) stats.wins++;
            }
        }
    }
}
