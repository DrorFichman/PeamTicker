package com.teampicker.drorfichman.teampicker.Data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Created by drorfichman on 10/3/16.
 */
public class GameDbHelper {

    private static final String SQL_CREATE_GAMES =
            "CREATE TABLE " + PlayerContract.GameEntry.TABLE_NAME + " (" +
                    PlayerContract.GameEntry.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    PlayerContract.GameEntry.GAME + " INTEGER, " +
                    PlayerContract.GameEntry.DATE + " TEXT, " +
                    PlayerContract.GameEntry.TEAM_RESULT + " INTEGER DEFAULT -1, " +
                    PlayerContract.GameEntry.TEAM1_SCORE + " INTEGER, " +
                    PlayerContract.GameEntry.TEAM2_SCORE + " INTEGER )";

    public static final String SQL_DROP_GAMES_TABLE =
            "DELETE FROM " + PlayerContract.GameEntry.TABLE_NAME;

    public static String getSqlCreate() {
        return SQL_CREATE_GAMES;
    }

    public static void insertGameResults(SQLiteDatabase db,
                                         Game g) {

        Log.d("TEAMS", "Saving result " + g.getScore());

        ContentValues values = new ContentValues();
        values.put(PlayerContract.GameEntry.GAME, g.gameId);
        values.put(PlayerContract.GameEntry.DATE, g.dateString);
        values.put(PlayerContract.GameEntry.TEAM1_SCORE, g.team1Score);
        values.put(PlayerContract.GameEntry.TEAM2_SCORE, g.team2Score);
        values.put(PlayerContract.GameEntry.TEAM_RESULT, g.winningTeam.ordinal());

        // Insert the new row, returning the primary key value of the new row
        db.insertWithOnConflict(PlayerContract.GameEntry.TABLE_NAME,
                null,
                values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void updateGameDate(SQLiteDatabase db, int gameId, String gameDate) {
        ContentValues values = new ContentValues();
        values.put(PlayerContract.GameEntry.DATE, gameDate);

        String where = PlayerContract.GameEntry.GAME + " = ? ";
        String[] whereArgs = new String[]{String.valueOf(gameId)};

        db.updateWithOnConflict(PlayerContract.GameEntry.TABLE_NAME,
                values,
                where, whereArgs, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /**
     * Unified games query.
     *
     * When {@code players} is empty: returns all games up to {@code limit} (≤0 = unlimited),
     * selecting only game-level columns.
     *
     * When {@code players} is non-empty: returns games where every listed player participated,
     * result columns read from the first player's perspective. {@code limit} is applied in cursor
     * traversal (≤0 = unlimited).
     */
    @NonNull
    public static ArrayList<Game> getGames(SQLiteDatabase db, List<String> players, int limit) {
        int count = limit > 0 ? limit : -1;

        if (players == null || players.isEmpty()) {
            String sortOrder = "date(date) DESC, game_index DESC";
            String[] projection = {
                    PlayerContract.GameEntry.ID,
                    PlayerContract.GameEntry.GAME,
                    PlayerContract.GameEntry.DATE,
                    PlayerContract.GameEntry.TEAM1_SCORE,
                    PlayerContract.GameEntry.TEAM2_SCORE,
            };
            Cursor c = db.query(PlayerContract.GameEntry.TABLE_NAME, projection,
                    null, null, null, null, sortOrder);
            return getGames(c, count);
        }

        StringBuilder sql = new StringBuilder(
                "SELECT game_index, g.date AS date," +
                " res1.date AS rdate, res1.result AS result, res1.player_grade AS player_grade," +
                " res1.attributes AS attributes, team_one_score, team_two_score,");

        // CASE expression: 1 when every player is on the same non-bench team
        if (players.size() >= 2) {
            sql.append(" CASE WHEN res1.team < 3"); // team < 3 excludes Bench (ordinal 3)
            for (int i = 1; i < players.size(); i++) {
                sql.append(" AND res1.team = res").append(i + 1).append(".team");
            }
            sql.append(" THEN 1 ELSE 0 END AS all_same_team");
        } else {
            sql.append(" 0 AS all_same_team");
        }

        sql.append(" FROM game g");
        for (int i = 0; i < players.size(); i++) {
            sql.append(", player_game res").append(i + 1);
        }

        sql.append(" WHERE res1.name = ? AND g.game_index = res1.game");
        for (int i = 1; i < players.size(); i++) {
            sql.append(" AND res").append(i + 1).append(".name = ?")
               .append(" AND g.game_index = res").append(i + 1).append(".game");
        }
        sql.append(" ORDER BY date(g.date) DESC, game_index DESC");

        Cursor c = db.rawQuery(sql.toString(), players.toArray(new String[0]));
        return getGames(c, count);
    }

    @NonNull
    public static ArrayList<Game> getGames(SQLiteDatabase db, List<String> players) {
        return getGames(db, players, -1);
    }

    @NonNull
    public static ArrayList<Game> getGames(SQLiteDatabase db, int limit) {
        return getGames(db, java.util.Collections.emptyList(), limit);
    }

    @NonNull
    private static ArrayList<Game> getGames(Cursor c, int count) {

        ArrayList<Game> games = new ArrayList<>();
        try {
            if (c.moveToFirst()) {
                int i = 0;
                do {
                    Game g = new Game(c.getInt(c.getColumnIndexOrThrow(PlayerContract.GameEntry.GAME)),
                            c.getString(c.getColumnIndexOrThrow(PlayerContract.GameEntry.DATE)),
                            c.getInt(c.getColumnIndexOrThrow(PlayerContract.GameEntry.TEAM1_SCORE)),
                            c.getInt(c.getColumnIndexOrThrow(PlayerContract.GameEntry.TEAM2_SCORE)));

                    int resultIdx = c.getColumnIndex(PlayerContract.PlayerGameEntry.PLAYER_RESULT);
                    if (resultIdx >= 0)
                        g.playerResult = ResultEnum.getResultFromOrdinal(c.getInt(resultIdx));
                    int gradeIdx = c.getColumnIndex(PlayerContract.PlayerGameEntry.PLAYER_GRADE);
                    if (gradeIdx >= 0)
                        g.playerGrade = c.getInt(gradeIdx);
                    
                    // Check if player was MVP or injured in this game
                    int attrIndex = c.getColumnIndex(PlayerContract.PlayerGameEntry.ATTRIBUTES);
                    if (attrIndex >= 0) {
                        String attributes = c.getString(attrIndex);
                        g.playerIsMVP = attributes != null && attributes.contains(PlayerAttribute.isMVP.displayName);
                        g.playerIsInjured = attributes != null && attributes.contains(PlayerAttribute.isInjured.displayName);
                    }

                    int sameTeamIndex = c.getColumnIndex("all_same_team");
                    if (sameTeamIndex >= 0) {
                        g.playersAllOnSameTeam = c.getInt(sameTeamIndex) == 1;
                    }

                    games.add(g);
                    i++;
                } while (c.moveToNext() && (i < count || count == -1));
            }
        } finally {
            c.close();
        }
        return games;
    }

    public static void deleteGame(SQLiteDatabase db, int gameId) {
        int delete = db.delete(PlayerContract.GameEntry.TABLE_NAME,
                PlayerContract.GameEntry.GAME + " = ? ",
                new String[]{String.valueOf(gameId)});
        Log.d("TEAMS", delete + " game was deleted");
    }

    /**
     * Count games with ID greater than the given ID
     * @param db the database
     * @param sinceGameId the game ID to start counting from (exclusive)
     * @return count of games with ID > sinceGameId
     */
    public static int countGamesSince(SQLiteDatabase db, int sinceGameId) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + PlayerContract.GameEntry.TABLE_NAME +
                        " WHERE " + PlayerContract.GameEntry.GAME + " > ?",
                new String[]{String.valueOf(sinceGameId)});
        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }
}
