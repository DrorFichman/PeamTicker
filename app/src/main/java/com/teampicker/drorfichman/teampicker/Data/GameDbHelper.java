package com.teampicker.drorfichman.teampicker.Data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

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

    @NonNull
    public static ArrayList<Game> getGames(SQLiteDatabase db, String name) {

        Cursor c = db.rawQuery("select " +
                        " game_index, g.date as date, " +
                        " res.date as rdate, res.result as result, res.player_grade as player_grade, " +
                        " res.attributes as attributes, " +
                        " team_one_score, " +
                        " team_two_score " +
                        " from game g, player_game res " +
                        " where name = ? " +
                        " AND game_index = res.game " +
                        " order by date(g.date) DESC, game_index DESC",
                new String[]{name}, null);

        return getGames(c, -1);
    }

    @NonNull
    public static ArrayList<Game> getGames(SQLiteDatabase db, String name, String another) {
        Cursor c = db.rawQuery("select " +
                        " game_index, g.date as date, " +
                        " res1.date as rdate, res1.result as result, res1.player_grade as player_grade, " +
                        " res1.attributes as attributes, " +
                        " team_one_score, " +
                        " team_two_score " +
                        " from game g, player_game res1, player_game res2  " +
                        " where res1.name = ? AND res2.name = ? " +
                        " AND game_index = res1.game AND game_index = res2.game " +
                        " order by date(g.date) DESC, game_index DESC ",
                new String[]{name, another}, null);

        return getGames(c, -1);
    }

    @NonNull
    public static ArrayList<Game> getGames(SQLiteDatabase db, int limit) {

        int count = limit > 0 ? limit : -1;

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                PlayerContract.GameEntry.ID,
                PlayerContract.GameEntry.GAME,
                PlayerContract.GameEntry.DATE,
                PlayerContract.GameEntry.TEAM1_SCORE,
                PlayerContract.GameEntry.TEAM2_SCORE,
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = "date(date) DESC, game_index DESC";

        Cursor c = db.query(
                PlayerContract.GameEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        return getGames(c, count);
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

                    if (c.getColumnIndex(PlayerContract.PlayerGameEntry.PLAYER_RESULT) > 0)
                        g.playerResult = ResultEnum.getResultFromOrdinal(c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.PLAYER_RESULT)));
                    if (c.getColumnIndex(PlayerContract.PlayerGameEntry.PLAYER_GRADE) > 0)
                        g.playerGrade = c.getInt(c.getColumnIndexOrThrow(PlayerContract.PlayerGameEntry.PLAYER_GRADE));
                    
                    // Check if player was MVP or injured in this game
                    int attrIndex = c.getColumnIndex(PlayerContract.PlayerGameEntry.ATTRIBUTES);
                    if (attrIndex >= 0) {
                        String attributes = c.getString(attrIndex);
                        g.playerIsMVP = attributes != null && attributes.contains(PlayerAttribute.isMVP.displayName);
                        g.playerIsInjured = attributes != null && attributes.contains(PlayerAttribute.isInjured.displayName);
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
}
