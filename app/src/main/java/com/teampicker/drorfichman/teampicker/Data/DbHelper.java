package com.teampicker.drorfichman.teampicker.Data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by drorfichman on 7/30/16.
 */
public class DbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 8;
    public static final String DATABASE_NAME = "Players.db";

    private static SQLiteDatabase writableDatabase;
    static HashMap<String, ArrayList<PlayerGameStat>> playersHistory = new HashMap<>();
    
    private final Context appContext;

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
    }

    public static void onUnderlyingDataChange() {
        playersHistory.clear();
    }

    /**
     * Save teams by updating the team column in the player table.
     * This stores the current game setup without committing to player_game history until results are saved.
     * The missedPlayers parameter is ignored here - missed players are only 
     * recorded when game results are finalized.
     */
    public static void saveTeams(Context ctx, ArrayList<Player> firstTeam,
                                 ArrayList<Player> secondTeam,
                                 ArrayList<Player> benchedPlayers,
                                 Collection<Player> missedPlayers,
                                 Collection<Player> mvpPlayers) {
        // Clear any existing team assignments
        clearActiveGame(ctx);

        SQLiteDatabase db = getSqLiteDatabase(ctx);
        
        for (Player a : firstTeam) {
            PlayerDbHelper.updatePlayerTeam(db, a.mName, TeamEnum.Team1);
        }
        for (Player b : secondTeam) {
            PlayerDbHelper.updatePlayerTeam(db, b.mName, TeamEnum.Team2);
        }
        if (benchedPlayers != null) {
            for (Player b : benchedPlayers) {
                PlayerDbHelper.updatePlayerTeam(db, b.mName, TeamEnum.Bench);
            }
        }
        // Note: missedPlayers are not stored here - they are only recorded
        // when the game is finalized with results in insertGame()
        // MVP attributes are stored in player_game when results are saved
    }

    private static String getAttributesWithMVP(Player player, Collection<Player> mvpPlayers) {
        String attributes = player.getAttributes();
        if (mvpPlayers != null && mvpPlayers.contains(player)) {
            if (attributes.isEmpty()) {
                attributes = PlayerAttribute.isMVP.displayName;
            } else {
                attributes = attributes + "," + PlayerAttribute.isMVP.displayName;
            }
        }
        return attributes;
    }

    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    public void createTables(SQLiteDatabase db) {
        try {
            db.execSQL(PlayerDbHelper.getSqlCreate());
            db.execSQL(PlayerGamesDbHelper.getSqlCreate());
            db.execSQL(GameDbHelper.getSqlCreate());
        } catch (SQLiteException e) {
            Log.w("Create", "Tables already exist " + e.getMessage());
        }

        Log.d("Create", "Create new tables called");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);

        addColumns(db);

        migrateEmptyResultRecords(db);

        // TODO sanitize player names for firebase, not mandatory - sanitized when synced to cloud
    }

    /*
    This content can be removed for official initial release - DDL done with the table
     */
    private void addColumns(SQLiteDatabase db) {
        addColumn(db, PlayerContract.PlayerEntry.TABLE_NAME, PlayerContract.PlayerEntry.BIRTH_YEAR, "INTEGER", null);
        addColumn(db, PlayerContract.PlayerEntry.TABLE_NAME, PlayerContract.PlayerEntry.BIRTH_MONTH, "INTEGER", null);
        addColumn(db, PlayerContract.PlayerGameEntry.TABLE_NAME, PlayerContract.PlayerGameEntry.PLAYER_AGE, "INTEGER", null);
        addColumn(db, PlayerContract.PlayerEntry.TABLE_NAME, PlayerContract.PlayerEntry.ARCHIVED, "INTEGER", "0");
        addColumn(db, PlayerContract.PlayerEntry.TABLE_NAME, PlayerContract.PlayerEntry.ATTRIBUTES, "TEXT", "''");
        addColumn(db, PlayerContract.PlayerGameEntry.TABLE_NAME, PlayerContract.PlayerGameEntry.ATTRIBUTES, "TEXT", "''");
        modifyGameDates(db);
        addColumn(db, PlayerContract.PlayerEntry.TABLE_NAME, PlayerContract.PlayerEntry.MSG_IDENTIFIER, "TEXT", "''");
        addColumn(db, PlayerContract.PlayerEntry.TABLE_NAME, PlayerContract.PlayerEntry.BIRTH_DAY, "INTEGER", null);
        // Team column for current game assignment (NULL=not assigned, 0=Team1, 1=Team2, 2=Bench)
        addColumn(db, PlayerContract.PlayerEntry.TABLE_NAME, PlayerContract.PlayerEntry.TEAM, "INTEGER", null);
    }

    /**
     * Migrate any pending game data (EMPTY_RESULT records) from player_game to player.team column.
     */
    private void migrateEmptyResultRecords(SQLiteDatabase db) {
        try {
            // Migrate EMPTY_RESULT records: update player.team from player_game
            db.execSQL("UPDATE " + PlayerContract.PlayerEntry.TABLE_NAME + 
                    " SET " + PlayerContract.PlayerEntry.TEAM + " = (" +
                    "SELECT " + PlayerContract.PlayerGameEntry.TEAM + " FROM " + PlayerContract.PlayerGameEntry.TABLE_NAME +
                    " WHERE " + PlayerContract.PlayerGameEntry.TABLE_NAME + "." + PlayerContract.PlayerGameEntry.NAME + 
                    " = " + PlayerContract.PlayerEntry.TABLE_NAME + "." + PlayerContract.PlayerEntry.NAME +
                    " AND " + PlayerContract.PlayerGameEntry.PLAYER_RESULT + " = " + PlayerGamesDbHelper.EMPTY_RESULT + ")");
            
            // Delete the migrated EMPTY_RESULT records from player_game
            int deleted = db.delete(PlayerContract.PlayerGameEntry.TABLE_NAME,
                    PlayerContract.PlayerGameEntry.PLAYER_RESULT + " = ?",
                    new String[]{String.valueOf(PlayerGamesDbHelper.EMPTY_RESULT)});
            
            if (deleted > 0) {
                Log.i("Migrate", "Migrated " + deleted + " pending records from player_game to player.team");
                Event.logEvent(FirebaseAnalytics.getInstance(appContext), EventType.db_migrate_empty_result);
            }
        } catch (Exception e) {
            Log.e("Migrate", "Error migrating EMPTY_RESULT records: " + e.getMessage(), e);
        }
    }

    private void addColumn(SQLiteDatabase db, String table, String column, String type, String def) {
        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type + " default " + def);
            Log.i("Upgrade", "Altering " + table + ": " + column);
        } catch (SQLiteException ex) {
            Log.e("Upgrade", "Altering " + table + ": " + ex.getMessage(), ex);
        }
    }

    /*
    Migrate existing data from date format that can't be ordered using sqlite
    This method can be removed for official initial release
     */
    private void modifyGameDates(SQLiteDatabase db) {
        // From dd-MM-YYYY to yyyy-MM-dd
        try {
            db.execSQL("UPDATE " + PlayerContract.GameEntry.TABLE_NAME +
                    " SET date = " +
                    " (substr(date,7,4) || '-' || substr(date, 4,2) || '-' || substr(date,1,2)) " +
                    " where substr(date,3,1) = '-' and substr(date,6,1) = '-' ");

            db.execSQL("UPDATE " + PlayerContract.PlayerGameEntry.TABLE_NAME +
                    " SET date = " +
                    " (substr(date,7,4) || '-' || substr(date, 4,2) || '-' || substr(date,1,2)) " +
                    " where substr(date,3,1) = '-' and substr(date,6,1) = '-' ");

        } catch (Exception e) {
            Log.e("modifyGameDates", e.getMessage(), e);
        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    @SuppressWarnings("resource") // DbHelper is intentionally kept open for the app's lifetime
    public static SQLiteDatabase getSqLiteDatabase(Context context) {
        // Gets the data repository in write mode
        // Check both null and isOpen() to handle cases where database was closed
        if (writableDatabase == null || !writableDatabase.isOpen()) {
            DbHelper mDbHelper = new DbHelper(context.getApplicationContext());
            writableDatabase = mDbHelper.getWritableDatabase();
        }
        return writableDatabase;
    }

    public static void deleteTableContents(Context context) {
        getSqLiteDatabase(context).execSQL(PlayerDbHelper.SQL_DROP_PLAYER_TABLE);
        getSqLiteDatabase(context).execSQL(GameDbHelper.SQL_DROP_GAMES_TABLE);
        getSqLiteDatabase(context).execSQL(PlayerGamesDbHelper.SQL_DROP_PLAYER_GAMES_TABLE);
    }

    public static void setPlayerComing(Context context, ArrayList<Player> team) {
        for (Player p : team) {
            PlayerDbHelper.updatePlayerComing(getSqLiteDatabase(context), p.mName, true);
        }
    }

    public static void updatePlayerComing(Context context, String name, boolean isComing) {
        PlayerDbHelper.updatePlayerComing(getSqLiteDatabase(context), name, isComing);
    }

    public static void updatePlayerGrade(Context context, String name, int grade) {
        PlayerDbHelper.updatePlayerGrade(getSqLiteDatabase(context), name, grade);
    }

    public static boolean updatePlayerName(Context context, Player player, String newName) {
        if (getPlayer(context, newName) != null) // new name already exists
            return false;

        PlayerDbHelper.updatePlayerName(getSqLiteDatabase(context), player.mName, newName);
        player.mName = newName;

        return true;
    }

    public static void updatePlayerAttributes(Context context, Player p) {
        PlayerDbHelper.setPlayerAttributes(getSqLiteDatabase(context), p.mName, p.getAttributes());
    }

    public static void updatePlayerBirth(Context context, String name, int year, int month, int day) {
        PlayerDbHelper.updatePlayerBirth(getSqLiteDatabase(context), name, year, month, day);
    }

    public static boolean insertPlayer(Context context, Player p) {
        p.mName = FirebaseHelper.sanitizeKey(p.mName);
        if (getPlayer(context, p.mName) != null) return false;
        return PlayerDbHelper.insertPlayer(getSqLiteDatabase(context), p);
    }

    public static Player getPlayer(Context context, String name) {
        return getPlayer(context, name, -1);
    }

    public static Player getPlayer(Context context, String name, int gameCount) {
        final Player player = PlayerDbHelper.getPlayer(getSqLiteDatabase(context), name);
        if (player != null) {
            addLastGameStats(context, gameCount, Collections.singletonList(player), true);
        }
        return player;
    }

    @NonNull
    public static ArrayList<Player> getPlayersStatistics(Context context, int games) {
        return PlayerGamesDbHelper.getPlayersStatistics(getSqLiteDatabase(context), games);
    }

    @NonNull
    public static HashMap<String, PlayerChemistry> getPlayersParticipationStatistics(Context context, String name, BuilderPlayerCollaborationStatistics params) {
        return getPlayersParticipationStatistics(context, params.games, params.cache, name);
    }

    @NonNull
    public static HashMap<String, PlayerChemistry> getPlayersParticipationStatistics(Context context, int games, GamesPlayersCache cache, String name) {
        return PlayerGamesDbHelper.getParticipationStatistics(getSqLiteDatabase(context), games, cache, name);
    }

    @NonNull
    public static ArrayList<Player> getPlayers(Context context) {
        return PlayerDbHelper.getPlayers(getSqLiteDatabase(context));
    }

    @NonNull
    public static ArrayList<Player> getPlayers(Context context, int gamesCount, boolean showArchived) {
        ArrayList<Player> players = PlayerDbHelper.getPlayers(getSqLiteDatabase(context), showArchived);
        DbHelper.addLastGameStats(context, gamesCount, players, false);
        return players;
    }

    @NonNull
    public static ArrayList<Player> getInjuredPlayers(Context context, int gamesCount) {
        ArrayList<Player> players = PlayerDbHelper.getInjuredPlayers(getSqLiteDatabase(context));
        DbHelper.addLastGameStats(context, gamesCount, players, false);
        return players;
    }

    @NonNull
    public static String[] getPlayersNames(Context ctx) {
        ArrayList<Player> players = DbHelper.getPlayers(ctx, 0, false);
        String[] allPlayerNames = new String[players.size()];
        int i = 0;
        for (Player p : players) {
            allPlayerNames[i] = p.mName;
            i++;
        }
        return allPlayerNames;
    }

    @NonNull
    public static ArrayList<Player> getPlayersByIdentifier(Context context, ArrayList<String> names) {
        return PlayerDbHelper.getPlayersByIdentifier(getSqLiteDatabase(context), names);
    }

    public static int getComingPlayersCount(Context context) {
        return PlayerDbHelper.getComingPlayersCount(getSqLiteDatabase(context));
    }

    @NonNull
    public static ArrayList<Player> getComingPlayers(Context context, int countLastGames) {
        ArrayList<Player> comingPlayers = PlayerDbHelper.getComingPlayers(getSqLiteDatabase(context));
        addLastGameStats(context, countLastGames, comingPlayers, countLastGames > 0);
        return comingPlayers;
    }

    public static void deletePlayer(Context context, String playerName) {
        PlayerDbHelper.deletePlayer(getSqLiteDatabase(context), playerName);
    }

    public static void archivePlayer(Context context, String name, boolean archiveValue) {
        PlayerDbHelper.setPlayerArchive(getSqLiteDatabase(context), name, archiveValue);
    }

    public static int setPlayerIdentifier(Context context, String name, String identifier) {
        return PlayerDbHelper.setPlayerMsgIdentifier(getSqLiteDatabase(context), name, identifier);
    }

    public static void clearPlayerIdentifier(Context context, String name) {
        PlayerDbHelper.clearPlayerMsgIdentifier(getSqLiteDatabase(context), name);
    }

    /**
     * Clear the active game setup. This clears all team assignments in the player table,
     * leaving historical player_game records untouched.
     */
    public static void clearActiveGame(Context context) {
        PlayerDbHelper.clearAllTeams(getSqLiteDatabase(context));
    }

    public static void insertPlayerGame(Context context, PlayerGame pg) {
        pg.playerName = FirebaseHelper.sanitizeKey(pg.playerName);
        PlayerGamesDbHelper.addPlayerGame(getSqLiteDatabase(context), pg);
    }

    @NonNull
    public static ArrayList<PlayerGame> getPlayersGames(Context ctx) {
        return PlayerGamesDbHelper.getPlayersGames(getSqLiteDatabase(ctx));
    }

    @NonNull
    public static ArrayList<PlayerGameStat> getPlayerLastGames(Context context, Player player, int countLastGames) {
        return PlayerGamesDbHelper.getPlayerLastGames(getSqLiteDatabase(context), player, countLastGames);
    }

    /**
     * Get players assigned to a team for the active game setup.
     * Reads from the player.team column.
     * 
     * @param context Context
     * @param team Which team to get (Team1, Team2, or Bench)
     * @param countLastGames Number of last games to include in player stats
     * @return List of players on the requested team
     */
    @NonNull
    public static ArrayList<Player> getActiveGameTeam(Context context, TeamEnum team, int countLastGames) {
        ArrayList<Player> players = PlayerDbHelper.getPlayersByTeam(getSqLiteDatabase(context), team);
        addLastGameStats(context, countLastGames, players, countLastGames > 0);
        return players;
    }

    /**
     * Get players from a completed/historical game.
     * Reads from the player_game table.
     * 
     * @param context Context
     * @param gameId The game ID to retrieve
     * @param team Which team to get (Team1, Team2, or Bench)
     * @param countLastGames Number of last games to include in player stats
     * @return List of players on the requested team
     */
    @NonNull
    public static ArrayList<Player> getGameTeam(Context context, int gameId, TeamEnum team, int countLastGames) {
        ArrayList<Player> players = PlayerGamesDbHelper.getGameTeam(getSqLiteDatabase(context), gameId, team);
        addLastGameStats(context, countLastGames, players, countLastGames > 0);
        return players;
    }

    private static void addLastGameStats(Context context, int countLastGames, List<Player> currTeam, boolean statistics) {

        for (Player p : currTeam) {
            if (playersHistory.containsKey(p.mName + countLastGames)) {
                p.results = playersHistory.get(p.mName + countLastGames);
            } else {
                p.results = PlayerGamesDbHelper.getPlayerLastGames(getSqLiteDatabase(context), p, countLastGames);
                playersHistory.put(p.mName + countLastGames, p.results);
            }
            if (statistics) {
                p.statistics = PlayerGamesDbHelper.getPlayerStatistics(getSqLiteDatabase(context), countLastGames, p.mName);
            }
        }
    }

    @NonNull
    public static ArrayList<Game> getGames(Context context) {
        return GameDbHelper.getGames(getSqLiteDatabase(context), -1);
    }

    @NonNull
    public static ArrayList<Game> getGames(Context context, int limit) {
        return GameDbHelper.getGames(getSqLiteDatabase(context), limit);
    }

    @NonNull
    public static ArrayList<Game> getGames(Context context, String name) {
        return GameDbHelper.getGames(getSqLiteDatabase(context), name);
    }

    @NonNull
    public static ArrayList<Game> getGames(Context context, String name, String another) {
        return GameDbHelper.getGames(getSqLiteDatabase(context), name, another);
    }

    /**
     * Finalize the active game with results. This reads players from player.team column,
     * writes them to player_game with results, and clears team assignments.
     */
    public static void insertGame(Context context, Game game) {
        insertGame(context, game, null);
    }

    /**
     * Finalize the active game with results and record missed players.
     * Reads players from player.team column, writes to player_game, then clears assignments.
     */
    public static void insertGame(Context context, Game game, Collection<Player> missedPlayers) {
        SQLiteDatabase db = getSqLiteDatabase(context);
        
        // Insert the game record
        GameDbHelper.insertGameResults(db, game);
        
        // Get players from each team and write to player_game with results
        finalizeActiveGame(db, game.gameId, game.dateString, game.winningTeam);
        
        // Record missed players
        if (missedPlayers != null) {
            for (Player m : missedPlayers) {
                PlayerGamesDbHelper.updatePlayerResultMissed(db, game.gameId, m.mName);
            }
        }
        
        onUnderlyingDataChange();
    }

    /**
     * Transfer players from player.team column to player_game table with final results.
     */
    private static void finalizeActiveGame(SQLiteDatabase db, int gameId, String dateString, TeamEnum winningTeam) {
        // Process Team1
        ArrayList<Player> team1 = PlayerDbHelper.getPlayersByTeam(db, TeamEnum.Team1);
        ResultEnum team1Result = getResultForTeam(TeamEnum.Team1, winningTeam);
        for (Player p : team1) {
            PlayerGame pg = new PlayerGame(gameId, p.mName, p.mGrade, TeamEnum.Team1, p.getAge(), p.getAttributes());
            pg.date = dateString;
            pg.result = team1Result;
            PlayerGamesDbHelper.addPlayerGame(db, pg);
        }
        
        // Process Team2
        ArrayList<Player> team2 = PlayerDbHelper.getPlayersByTeam(db, TeamEnum.Team2);
        ResultEnum team2Result = getResultForTeam(TeamEnum.Team2, winningTeam);
        for (Player p : team2) {
            PlayerGame pg = new PlayerGame(gameId, p.mName, p.mGrade, TeamEnum.Team2, p.getAge(), p.getAttributes());
            pg.date = dateString;
            pg.result = team2Result;
            PlayerGamesDbHelper.addPlayerGame(db, pg);
        }
        
        // Process Bench (result is NA)
        ArrayList<Player> bench = PlayerDbHelper.getPlayersByTeam(db, TeamEnum.Bench);
        for (Player p : bench) {
            PlayerGame pg = new PlayerGame(gameId, p.mName, p.mGrade, TeamEnum.Bench, p.getAge(), p.getAttributes());
            pg.date = dateString;
            pg.result = ResultEnum.NA;
            PlayerGamesDbHelper.addPlayerGame(db, pg);
        }
        
        // Clear all team assignments
        PlayerDbHelper.clearAllTeams(db);
        Log.d("DbHelper", "Finalized game " + gameId + " with " + 
                (team1.size() + team2.size() + bench.size()) + " players");
    }

    /**
     * Determine the result for a team based on the winning team.
     */
    private static ResultEnum getResultForTeam(TeamEnum team, TeamEnum winningTeam) {
        if (winningTeam == null || winningTeam == TeamEnum.Bench) {
            return ResultEnum.Tie;
        } else if (team == winningTeam) {
            return ResultEnum.Win;
        } else {
            return ResultEnum.Lose;
        }
    }

    public static void updateGameDate(Context context, Game game, String gameDate) {
        GameDbHelper.updateGameDate(getSqLiteDatabase(context), game.gameId, gameDate);
        PlayerGamesDbHelper.updateGameDate(getSqLiteDatabase(context), game.gameId, gameDate);
    }

    public static void setPlayerResultMissed(Context context, int gameId, String name) {
        PlayerGamesDbHelper.updatePlayerResultMissed(getSqLiteDatabase(context), gameId, name);
    }

    public static int updateRecord(SQLiteDatabase db, ContentValues values, String where, String[] whereArgs, String tableName) {

        return db.updateWithOnConflict(tableName,
                values,
                where, whereArgs, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static void deleteGame(Context context, int game) {

        GameDbHelper.deleteGame(getSqLiteDatabase(context), game);
        PlayerGamesDbHelper.deleteGame(getSqLiteDatabase(context), game);
        onUnderlyingDataChange();
    }

    public static void clearComingPlayers(Context context) {
        PlayerDbHelper.clearAllComing(getSqLiteDatabase(context));
    }

    public static int getMaxGame(Context context) {
        return PlayerGamesDbHelper.getMaxGame(getSqLiteDatabase(context));
    }

    /**
     * Get the next game ID to use when creating a new game.
     * This is always max + 1, regardless of whether there's an active game setup.
     */
    public static int getNextGameId(Context context) {
        return getMaxGame(context) + 1;
    }

    /**
     * Check if there's an active game setup in progress (any player has a team assignment).
     */
    public static boolean hasActiveGame(Context context) {
        return PlayerDbHelper.hasActiveGame(getSqLiteDatabase(context));
    }

    @NonNull
    public static StreakInfo getLongestUnbeatenRun(Context context, String playerName) {
        return PlayerGamesDbHelper.getLongestUnbeatenRun(getSqLiteDatabase(context), playerName);
    }

    @NonNull
    public static StreakInfo getConsecutiveAttendance(Context context, String playerName) {
        return PlayerGamesDbHelper.getConsecutiveAttendance(getSqLiteDatabase(context), playerName);
    }

    /**
     * Count games that have not been synced to cloud yet.
     * @param context Context
     * @param lastSyncedGameId the last game ID that was synced
     * @return count of games with ID > lastSyncedGameId
     */
    public static int countUnsyncedGames(Context context, int lastSyncedGameId) {
        return GameDbHelper.countGamesSince(getSqLiteDatabase(context), lastSyncedGameId);
    }
}

