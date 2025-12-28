package com.teampicker.drorfichman.teampicker.Data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import com.teampicker.drorfichman.teampicker.tools.DateHelper;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by drorfichman on 10/3/16.
 */
public class PlayerGamesDbHelper {

    // TODO move ResultEnum, check getValue
    public static final int EMPTY_RESULT = -10;
    public static final int MISSED_GAME = -9;
    public static final int TIE = 0;
    public static final int WIN = 1;
    public static final int LOSE = -1;

    private static final String SQL_CREATE_PLAYERS_GAMES =
            "CREATE TABLE " + PlayerContract.PlayerGameEntry.TABLE_NAME + " (" +
                    PlayerContract.PlayerGameEntry.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    PlayerContract.PlayerGameEntry.NAME + " TEXT, " +
                    PlayerContract.PlayerGameEntry.GAME + " INTEGER, " +
                    PlayerContract.PlayerGameEntry.DATE + " TEXT, " +
                    PlayerContract.PlayerGameEntry.PLAYER_GRADE + " INTEGER, " +
                    PlayerContract.PlayerGameEntry.PLAYER_AGE + " INTEGER, " +
                    PlayerContract.PlayerGameEntry.GAME_GRADE + " INTEGER, " +
                    PlayerContract.PlayerGameEntry.TEAM + " INTEGER, " +
                    PlayerContract.PlayerGameEntry.GOALS + " INTEGER, " +
                    PlayerContract.PlayerGameEntry.ASSISTS + " INTEGER, " +
                    PlayerContract.PlayerGameEntry.DID_WIN + " INTEGER, " +
                    PlayerContract.PlayerGameEntry.PLAYER_RESULT + " INTEGER DEFAULT " + EMPTY_RESULT + ", " +
                    PlayerContract.PlayerGameEntry.ATTRIBUTES + " TEXT DEFAULT '' " +
                    ")";

    public static final String SQL_DROP_PLAYER_GAMES_TABLE =
            "DELETE FROM " + PlayerContract.PlayerGameEntry.TABLE_NAME;

    public static String getSqlCreate() {
        return SQL_CREATE_PLAYERS_GAMES;
    }

    public static void addPlayerGame(SQLiteDatabase db, PlayerGame pg) {

        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerGameEntry.GAME, pg.gameId);
        values.put(PlayerContract.PlayerGameEntry.DATE, pg.date != null ? pg.date : DateHelper.getNow());
        values.put(PlayerContract.PlayerGameEntry.NAME, pg.playerName);
        values.put(PlayerContract.PlayerGameEntry.PLAYER_GRADE, pg.playerGrade);
        if (pg.team != null) values.put(PlayerContract.PlayerGameEntry.TEAM, pg.team.ordinal()); // TODO temp
        values.put(PlayerContract.PlayerGameEntry.PLAYER_AGE, pg.playerAge);
        values.put(PlayerContract.PlayerGameEntry.ATTRIBUTES, pg.attributes);

        if (pg.result != null) {
            values.put(PlayerContract.PlayerGameEntry.PLAYER_RESULT, pg.result.getValue());
            values.put(PlayerContract.PlayerGameEntry.DID_WIN, pg.result == ResultEnum.Win ? 1 : 0);
        }

        // Insert the new row, returning the primary key value of the new row
        db.insert(PlayerContract.PlayerGameEntry.TABLE_NAME,
                null,
                values);
    }

    @NonNull
    public static ArrayList<Player> getCurrTeam(SQLiteDatabase db, int currGame, TeamEnum team) {

        String[] projection = {
                PlayerContract.PlayerGameEntry.ID,
                PlayerContract.PlayerGameEntry.NAME,
                PlayerContract.PlayerGameEntry.GAME,
                PlayerContract.PlayerGameEntry.PLAYER_GRADE,
                PlayerContract.PlayerGameEntry.PLAYER_AGE,
                PlayerContract.PlayerGameEntry.GAME_GRADE,
                PlayerContract.PlayerGameEntry.GOALS,
                PlayerContract.PlayerGameEntry.ASSISTS,
                PlayerContract.PlayerGameEntry.TEAM,
                PlayerContract.PlayerGameEntry.PLAYER_RESULT,
                PlayerContract.PlayerGameEntry.ATTRIBUTES
        };

        String sortOrder = PlayerContract.PlayerGameEntry.PLAYER_GRADE + " DESC";

        String where = PlayerContract.PlayerGameEntry.TEAM + " = ? AND " +
                PlayerContract.PlayerGameEntry.GAME + " = ? ";
        String[] whereArgs = new String[]{
                String.valueOf(team.ordinal()),
                String.valueOf(currGame)};

        Cursor c = db.query(
                PlayerContract.PlayerGameEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                where,                                // The columns for the WHERE clause
                whereArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        ArrayList<Player> players = new ArrayList<>();
        try {
            if (c.moveToFirst()) {
                do {
                    // TODO set rest of the fields - goals, grades...
                    Player p = PlayerDbHelper.createPlayerFromCursor(c,
                            PlayerContract.PlayerGameEntry.NAME,
                            null,
                            null,
                            null,
                            null,
                            PlayerContract.PlayerGameEntry.PLAYER_GRADE,
                            null, null, PlayerContract.PlayerGameEntry.ATTRIBUTES);

                    // age is saved at the time the game was played
                    p.setAge(c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.PLAYER_AGE)));
                    p.gameResult = c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.PLAYER_RESULT));
                    
                    // Check if player was MVP in this game
                    int attrIndex = c.getColumnIndex(PlayerContract.PlayerGameEntry.ATTRIBUTES);
                    if (attrIndex >= 0) {
                        String gameAttrs = c.getString(attrIndex);
                        p.gameIsMVP = gameAttrs != null && gameAttrs.contains(PlayerAttribute.isMVP.displayName);
                    }

                    players.add(p);
                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

        return players;
    }

    public static void clearOldGameTeams(SQLiteDatabase db) {
        Log.d("teams", "Clear old Game teams ");

        // Delete empty result (-10), missed result (-9), and null result rows
        // This ensures missed player records don't create orphaned entries
        int n = db.delete(PlayerContract.PlayerGameEntry.TABLE_NAME,
                PlayerContract.PlayerGameEntry.PLAYER_RESULT + " IS NULL OR " +
                        PlayerContract.PlayerGameEntry.PLAYER_RESULT + " = ? OR " +
                        PlayerContract.PlayerGameEntry.PLAYER_RESULT + " = ? ",
                new String[]{String.valueOf(PlayerGamesDbHelper.EMPTY_RESULT), 
                        String.valueOf(PlayerGamesDbHelper.MISSED_GAME)});

        Log.d("teams", "deleted games players " + n);
    }

    @NonNull
    public static ArrayList<PlayerGame> getPlayersGames(SQLiteDatabase db) {

        ArrayList<PlayerGame> playersGames = new ArrayList<>();

        String[] projection = {
                PlayerContract.PlayerGameEntry.NAME,
                PlayerContract.PlayerGameEntry.GAME,
                PlayerContract.PlayerGameEntry.DATE,
                PlayerContract.PlayerGameEntry.PLAYER_GRADE,
                PlayerContract.PlayerGameEntry.PLAYER_AGE,
                PlayerContract.PlayerGameEntry.TEAM,
                PlayerContract.PlayerGameEntry.PLAYER_RESULT,
                PlayerContract.PlayerGameEntry.DID_WIN
        };

        String sortOrder = "date(" + PlayerContract.PlayerGameEntry.DATE + ") DESC, " 
                + PlayerContract.PlayerGameEntry.GAME + " DESC";

        // The table to query
        // The columns to return
        // The columns for the WHERE clause
        // The values for the WHERE clause
        // don't group the rows
        // don't filter by row groups
        // The sort order

        try (Cursor c = db.query(
                PlayerContract.PlayerGameEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        )) {
            if (c.moveToFirst()) {
                do {
                    PlayerGame g = new PlayerGame();
                    g.playerGrade = c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.PLAYER_GRADE));
                    g.date = c.getString(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.DATE));
                    g.playerAge = c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.PLAYER_AGE));
                    g.playerName = FirebaseHelper.sanitizeKey(c.getString(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.NAME)));
                    g.gameId = c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.GAME));
                    g.didWin = c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.DID_WIN));

                    int res = c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.PLAYER_RESULT));
                    g.result = ResultEnum.getResultFromOrdinal(res);
                    int team = c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.TEAM));
                    g.team = TeamEnum.getResultFromOrdinal(team);

                    playersGames.add(g);
                } while (c.moveToNext());
            }
        }

        return playersGames;
    }

    @NonNull
    public static ArrayList<PlayerGameStat> getPlayerLastGames(SQLiteDatabase db, Player player, int countLastGames) {

        ArrayList<PlayerGameStat> results = new ArrayList<>();
        if (countLastGames <= 0) {
            return results;
        }

        StringBuilder debugLog = new StringBuilder();
        debugLog.append("getPlayerLastGames for ").append(player.mName).append(": ");

        // Join with game table to only return results for games that actually exist
        // This filters out orphaned player_game records from deleted games
        String query = "SELECT pg." + PlayerContract.PlayerGameEntry.GAME + ", " +
                "pg." + PlayerContract.PlayerGameEntry.DATE + ", " +
                "pg." + PlayerContract.PlayerGameEntry.PLAYER_RESULT + ", " +
                "pg." + PlayerContract.PlayerGameEntry.PLAYER_GRADE + ", " +
                "pg." + PlayerContract.PlayerGameEntry.ATTRIBUTES +
                " FROM " + PlayerContract.PlayerGameEntry.TABLE_NAME + " pg " +
                " INNER JOIN " + PlayerContract.GameEntry.TABLE_NAME + " g " +
                " ON pg." + PlayerContract.PlayerGameEntry.GAME + " = g." + PlayerContract.GameEntry.GAME +
                " WHERE pg." + PlayerContract.PlayerGameEntry.NAME + " = ? " +
                " AND pg." + PlayerContract.PlayerGameEntry.PLAYER_RESULT + " > ? " +
                " ORDER BY date(pg." + PlayerContract.PlayerGameEntry.DATE + ") DESC, " +
                "pg." + PlayerContract.PlayerGameEntry.GAME + " DESC";

        try (Cursor c = db.rawQuery(query, new String[]{player.mName, String.valueOf(EMPTY_RESULT)})) {
            if (c.moveToFirst()) {
                int i = 0;
                do {
                    int res = c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.PLAYER_RESULT));
                    int grade = c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.PLAYER_GRADE));
                    String date = c.getString(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.DATE));

                    // Check if player was MVP in this game
                    boolean isMVP = false;
                    int attrIndex = c.getColumnIndex(PlayerContract.PlayerGameEntry.ATTRIBUTES);
                    if (attrIndex >= 0) {
                        String attributes = c.getString(attrIndex);
                        isMVP = attributes != null && attributes.contains(PlayerAttribute.isMVP.displayName);
                    }

                    ResultEnum resultEnum = ResultEnum.getResultFromOrdinal(res);
                    PlayerGameStat stat = new PlayerGameStat(resultEnum, grade, date, isMVP);

                    results.add(stat);
                    i++;
                } while (c.moveToNext() && (i < countLastGames));
            }
        }

        results.sort(Comparator.comparing(PlayerGameStat::getDate).reversed());
        return results;
    }

    public static void setPlayerGameResult(SQLiteDatabase db, int gameId, String date, TeamEnum winningTeam) {

        updateTeamGameResult(db, gameId, date, TeamEnum.Team1, TeamEnum.getTeam1Result(winningTeam));
        updateTeamGameResult(db, gameId, date, TeamEnum.Team2, TeamEnum.getTeam2Result(winningTeam));
    }

    public static void updatePlayerResultMissed(SQLiteDatabase db, int gameId, String name) {
        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerGameEntry.PLAYER_RESULT, ResultEnum.Missed.getValue());
        values.put(PlayerContract.PlayerGameEntry.DID_WIN, 0);

        String where = PlayerContract.PlayerGameEntry.GAME + " = ? AND " +
                PlayerContract.PlayerGameEntry.NAME + " = ? ";
        String[] whereArgs = new String[]{String.valueOf(gameId), name};

        // Update the new row, returning the primary key value of the new row
        DbHelper.updateRecord(db, values, where, whereArgs, PlayerContract.PlayerGameEntry.TABLE_NAME);
    }

    private static void updateTeamGameResult(SQLiteDatabase db, int gameId, String date, TeamEnum team, ResultEnum result) {

        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerGameEntry.PLAYER_RESULT, result.getValue());
        values.put(PlayerContract.PlayerGameEntry.DID_WIN, result == ResultEnum.Win ? 1 : 0);
        values.put(PlayerContract.PlayerGameEntry.DATE, date);

        String where = PlayerContract.PlayerGameEntry.GAME + " = ? AND " +
                PlayerContract.PlayerGameEntry.TEAM + " = ? AND " +
                PlayerContract.PlayerGameEntry.PLAYER_RESULT + " != ? ";
        String[] whereArgs = new String[]{String.valueOf(gameId),
                String.valueOf(team.ordinal()),
                String.valueOf(ResultEnum.Missed.getValue())}; // avoid overwrite 'missed'

        // Update the new row, returning the primary key value of the new row
        DbHelper.updateRecord(db, values, where, whereArgs, PlayerContract.PlayerGameEntry.TABLE_NAME);
    }

    public static void updateGameDate(SQLiteDatabase db, int gameId, String date) {

        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerGameEntry.DATE, date);

        String where = PlayerContract.PlayerGameEntry.GAME + " = ? ";
        String[] whereArgs = new String[]{String.valueOf(gameId)};

        DbHelper.updateRecord(db, values, where, whereArgs, PlayerContract.PlayerGameEntry.TABLE_NAME);
    }

    @NonNull
    public static ArrayList<Player> getPlayersStatistics(SQLiteDatabase db, int gameCount) {
        return getStatistics(db, gameCount, null);
    }

    public static StatisticsData getPlayerStatistics(SQLiteDatabase db, int gameCount, String name) {
        ArrayList<Player> playersStatistics = getStatistics(db, gameCount, name);
        if (!playersStatistics.isEmpty()) {
            return playersStatistics.get(0).statistics;
        } else {
            Log.w("STAT", "Can't find player stats " + name);
            return new StatisticsData();
        }
    }

    private static ArrayList<Player> getStatistics(SQLiteDatabase db, int gameCount, String name) {

        String limitGamesCount = "";
        Log.d("stats", "Game count " + gameCount);
        if (gameCount > 0) {
            limitGamesCount = " AND game in (select game_index from game order by date(date) DESC LIMIT " + gameCount + " ) ";
        }

        String nameFilter = "";
        String[] args = null;
        if (name != null) {
            nameFilter = " AND player.name =  ? ";
            args = new String[]{name};
        }

        Cursor c = db.rawQuery("select player.name as player_name, " +
                        " player.birth_year as birth_year, " +
                        " player.birth_month as birth_month, " +
                        " player.birth_day as birth_day, " +
                        " player.grade as player_grade, " +
                        " player.attributes as player_attributes, " +
                        " sum(result) as results_sum, " +
                        " sum(did_win) as results_wins, " +
                        " count(result) as results_count " +
                        " from player_game, player " +
                        " where " +
                        " player.name = player_game.name " + nameFilter +
                        " AND result IS NOT NULL " +
                        " AND result NOT IN ( " +
                        PlayerGamesDbHelper.EMPTY_RESULT + ", " +
                        PlayerGamesDbHelper.MISSED_GAME + " ) " +
                        limitGamesCount +
                        " group by player_name " +
                        " order by results_sum DESC",
                args, null);

        ArrayList<Player> players = new ArrayList<>();
        try {
            if (c.moveToFirst()) {
                do {
                    Player p = PlayerDbHelper.createPlayerFromCursor(c,
                            "player_name",
                            null,
                            "birth_year",
                            "birth_month",
                            "birth_day",
                            "player_grade",
                            null, null,
                            "player_attributes");

                    int games = c.getInt(c.getColumnIndexOrThrow("results_count"));
                    int success = c.getInt(c.getColumnIndexOrThrow("results_sum"));
                    int wins = c.getInt(c.getColumnIndexOrThrow("results_wins"));

                    StatisticsData s = new StatisticsData(games, success, wins);
                    p.setStatistics(s);

                    players.add(p);
                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

        return players;
    }

    public static void deleteGame(SQLiteDatabase db, int gameId) {
        int delete = db.delete(PlayerContract.PlayerGameEntry.TABLE_NAME,
                PlayerContract.PlayerGameEntry.GAME + " = ? ",
                new String[]{String.valueOf(gameId)});
        Log.d("TEAMS", delete + " game players were deleted");
    }

    private static PlayerChemistry getPlayer(HashMap<String, PlayerChemistry> result,
                                             String currName, String collaborator) {

        PlayerChemistry p = result.get(currName);
        if (p == null) {
            p = new PlayerChemistry();
            p.mName = currName;
            p.mParticipatedWith = collaborator;
            result.put(currName, p);
        }

        return p;
    }

    @NonNull
    public static HashMap<String, PlayerChemistry> getParticipationStatistics(SQLiteDatabase db, int gameCount, GamesPlayersCache cache, String name) {

        String limitGamesCount = "";
        if (gameCount > 0) {
            limitGamesCount = " AND game in (select game_index from game order by date(date) DESC LIMIT " + gameCount + " ) ";
        }

        String limitUpToDate = " ";
//        if (upTo != null) { // TODO add check valid time
//            limitUpToDate = " AND date < date(" + DateHelper.getDisplayDate(upTo.getTime()) + ") ";
//        }

        Cursor c = db.rawQuery("select player.name as player_name, " +
                        " game, team, result " +
                        " from player_game, player " +
                        " where " +
                        " player.name = player_game.name AND player.name =  ? " +
                        limitUpToDate +
                        " AND result IS NOT NULL " +
                        " AND result NOT IN ( " +
                        PlayerGamesDbHelper.EMPTY_RESULT + ", " +
                        PlayerGamesDbHelper.MISSED_GAME + " ) " +
                        limitGamesCount,
                new String[]{name}, null);

        HashMap<String, PlayerChemistry> result = new HashMap<>();

        // Get teams based on player active games, aggregate player wins with and against teammates
        try {
            if (c.moveToFirst()) {
                do {
                    int currGame = c.getInt(c.getColumnIndexOrThrow("game"));
                    ResultEnum gameResult = ResultEnum.getResultFromOrdinal(c.getInt(c.getColumnIndexOrThrow("result")));
                    int collaboratorTeam = c.getInt(c.getColumnIndexOrThrow("team"));

                    ArrayList<Player> team1;
                    ArrayList<Player> team2;

                    if (cache != null) {
                        team1 = cache.team1s.get(currGame);
                        team2 = cache.team2s.get(currGame);

                        if (team1 == null || team2 == null) {
                            team1 = getCurrTeam(db, currGame, TeamEnum.Team1);
                            team2 = getCurrTeam(db, currGame, TeamEnum.Team2);
                            cache.team1s.put(currGame, team1);
                            cache.team2s.put(currGame, team2);
                        }
                    } else {
                        team1 = getCurrTeam(db, currGame, TeamEnum.Team1);
                        team2 = getCurrTeam(db, currGame, TeamEnum.Team2);
                    }

                    if (TeamEnum.Team1.ordinal() == collaboratorTeam) {
                        for (Player p1 : team1) { // same team
                            PlayerChemistry player = getPlayer(result, p1.mName, name);
                            addResult(player.statisticsWith, gameResult, ResultEnum.getResultFromOrdinal(p1.gameResult));
                        }
                        for (Player p2 : team2) { // opposite team
                            PlayerChemistry player = getPlayer(result, p2.mName, name);
                            addResult(player.statisticsVs, gameResult, ResultEnum.getResultFromOrdinal(p2.gameResult));
                        }
                    }

                    if (TeamEnum.Team2.ordinal() == collaboratorTeam) {
                        for (Player p2 : team2) { // same team
                            PlayerChemistry player = getPlayer(result, p2.mName, name);
                            addResult(player.statisticsWith, gameResult, ResultEnum.getResultFromOrdinal(p2.gameResult));
                        }
                        for (Player p1 : team1) { // opposite team
                            PlayerChemistry player = getPlayer(result, p1.mName, name);
                            addResult(player.statisticsVs, gameResult, ResultEnum.getResultFromOrdinal(p1.gameResult));
                        }
                    }

                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

        // Remove the actual player
        result.remove(name);

        return result;
    }

    private static void addResult(StatisticsData playerStatistics,
                                  ResultEnum collaboratorRes, ResultEnum playerRes) {
        if (!ResultEnum.isActive(playerRes))
            return;

        playerStatistics.gamesCount++;
        if (ResultEnum.Win.equals(collaboratorRes)) {
            playerStatistics.wins++;
            playerStatistics.successRate++;
        }
        if (ResultEnum.Lose.equals(collaboratorRes)) {
            playerStatistics.successRate--;
        }
    }

    public static int getActiveGame(SQLiteDatabase db) {

        try (Cursor c = db.rawQuery("select game " +
                        " from player_game " +
                        " where " +
                        " result = " + PlayerGamesDbHelper.EMPTY_RESULT +
                        " order by game DESC ",
                null, null)) {
            if (c.moveToFirst()) {
                return c.getInt(c.getColumnIndexOrThrow("game"));
            }
        }

        return -1;
    }

    public static int getMaxGame(SQLiteDatabase db) {

        try (Cursor c = db.rawQuery("select max(game) as curr_game " +
                        " from player_game ",
                null, null)) {
            if (c.moveToFirst()) {
                return c.getInt(c.getColumnIndexOrThrow("curr_game"));
            }
        }

        return 1;
    }

    /**
     * Calculates the longest unbeaten run (consecutive games without a loss) for a player
     * @param db SQLiteDatabase instance
     * @param playerName Name of the player to check
     * @return The length of the longest unbeaten run
     */
    @NonNull
    public static StreakInfo getLongestUnbeatenRun(SQLiteDatabase db, String playerName) {

        try (Cursor c = db.rawQuery(
                "SELECT " +
                        "  " + PlayerContract.PlayerGameEntry.PLAYER_RESULT + " as player_result, " +
                        "  date " +
                        " FROM " + PlayerContract.PlayerGameEntry.TABLE_NAME + " " +
                        " WHERE name = ? " +
                        "  AND " + PlayerContract.PlayerGameEntry.PLAYER_RESULT + " IS NOT NULL " +
                        "  AND " + PlayerContract.PlayerGameEntry.PLAYER_RESULT + " != " + MISSED_GAME + " " +
                        " ORDER BY date, " + PlayerContract.PlayerGameEntry.ID + " ASC",
                new String[]{playerName}
        )) {
            int currentStreak = 0;
            int longestStreak = 0;
            String streakStartDate = null;
            String currentStreakStartDate = null;
            String streakEndDate = null;

            if (c.moveToFirst()) {
                do {
                    int result = c.getInt(c.getColumnIndexOrThrow("player_result"));
                    String date = c.getString(c.getColumnIndexOrThrow("date"));

                    if (result == LOSE) {
                        // Reset streak on loss
                        currentStreak = 0;
                        currentStreakStartDate = null;
                    } else if (result == WIN || result == TIE) {
                        // Start new streak or continue existing one
                        if (currentStreak == 0) {
                            currentStreakStartDate = date;
                        }
                        currentStreak++;

                        // Update longest streak if current streak is longer
                        if (currentStreak > longestStreak) {
                            longestStreak = currentStreak;
                            streakStartDate = currentStreakStartDate;
                            streakEndDate = date;
                        }
                    }
                } while (c.moveToNext());
            }

            if (longestStreak > 0) {
                return new StreakInfo(longestStreak, streakStartDate, streakEndDate);
            }
        }

        return new StreakInfo(0, null, null);
    }

    @NonNull
    public static StreakInfo getConsecutiveAttendance(SQLiteDatabase db, String playerName) {
        // First get all unique game dates
        try (Cursor gamesDates = db.rawQuery(
                "SELECT DISTINCT " + PlayerContract.PlayerGameEntry.DATE +
                " FROM " + PlayerContract.PlayerGameEntry.TABLE_NAME + " " +
                " ORDER BY date ASC",
                null
        )) {
            // Get all player's game dates into a HashSet for O(1) lookup
            HashSet<String> playerGameDates = new HashSet<>();
            try (Cursor playerGamesCursor = db.rawQuery(
                    "SELECT DISTINCT " + PlayerContract.PlayerGameEntry.DATE +
                    " FROM " + PlayerContract.PlayerGameEntry.TABLE_NAME + " " +
                    " WHERE name = ? " +
                    "  AND " + PlayerContract.PlayerGameEntry.PLAYER_RESULT + " IS NOT NULL " +
                    "  AND " + PlayerContract.PlayerGameEntry.PLAYER_RESULT + " IS NOT " + PlayerGamesDbHelper.EMPTY_RESULT +
                    "  AND " + PlayerContract.PlayerGameEntry.PLAYER_RESULT + " IS NOT " + PlayerGamesDbHelper.MISSED_GAME,
                    new String[]{playerName}
            )) {
                if (playerGamesCursor.moveToFirst()) {
                    do {
                        playerGameDates.add(
                                playerGamesCursor.getString(playerGamesCursor.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.DATE)));
                    } while (playerGamesCursor.moveToNext());
                }
            }

            int currentStreak = 0;
            int longestStreak = 0;
            String streakStartDate = null;
            String currentStreakStartDate = null;
            String streakEndDate = null;

            if (gamesDates.moveToFirst()) {
                do {
                    String currentGameDate = gamesDates.getString(gamesDates.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.DATE));
                    boolean hadGameOnCurrentDate = playerGameDates.contains(currentGameDate);

                    if (hadGameOnCurrentDate) {
                        if (currentStreak == 0) {
                            currentStreakStartDate = currentGameDate;
                        }
                        currentStreak++;

                        // Update longest streak if current streak is longer
                        if (currentStreak > longestStreak) {
                            longestStreak = currentStreak;
                            streakStartDate = currentStreakStartDate;
                            streakEndDate = currentGameDate;
                        }
                    } else {
                        // Reset streak if player missed this date
                        currentStreak = 0;
                        currentStreakStartDate = null;
                    }
                } while (gamesDates.moveToNext());
            }

            if (longestStreak > 0) {
                return new StreakInfo(longestStreak, streakStartDate, streakEndDate);
            }
        }

        return new StreakInfo(0, null, null);
    }
}