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
public class PlayerDbHelper {

    private static final String SQL_CREATE_PLAYERS =
            "CREATE TABLE " + PlayerContract.PlayerEntry.TABLE_NAME + " (" +
                    PlayerContract.PlayerEntry.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    PlayerContract.PlayerEntry.NAME + " TEXT, " +
                    PlayerContract.PlayerEntry.GRADE + " INTEGER, " +
                    PlayerContract.PlayerEntry.IS_COMING + " INTEGER, " +
                    PlayerContract.PlayerEntry.BIRTH_YEAR + " INTEGER, " +
                    PlayerContract.PlayerEntry.BIRTH_MONTH + " INTEGER, " +
                    PlayerContract.PlayerEntry.ARCHIVED + " INTEGER DEFAULT 0, " +
                    PlayerContract.PlayerEntry.ATTRIBUTES + " TEXT DEFAULT '', " +
                    PlayerContract.PlayerEntry.MSG_IDENTIFIER + " TEXT DEFAULT '' " +
                    " )";

    public static final String SQL_DROP_PLAYER_TABLE =
            "DELETE FROM " + PlayerContract.PlayerEntry.TABLE_NAME;

    public static String getSqlCreate() {
        return SQL_CREATE_PLAYERS;
    }

    public static int getComingPlayersCount(SQLiteDatabase db) {
        String[] projection = {
                PlayerContract.PlayerEntry.ID,
                PlayerContract.PlayerEntry.IS_COMING
        };

        String where = PlayerContract.PlayerEntry.IS_COMING + " = ? AND " +
                PlayerContract.PlayerEntry.ARCHIVED + " = ? ";
        String[] whereArgs = new String[]{"1", "0"}; // coming and not archived

        Cursor c = db.query(
                PlayerContract.PlayerEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                where,                                // The columns for the WHERE clause
                whereArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );

        int count = c.getCount();
        c.close();

        return count;
    }

    public static
    @NonNull
    ArrayList<Player> getComingPlayers(SQLiteDatabase db) {

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                PlayerContract.PlayerEntry.ID,
                PlayerContract.PlayerEntry.NAME,
                PlayerContract.PlayerEntry.MSG_IDENTIFIER,
                PlayerContract.PlayerEntry.GRADE,
                PlayerContract.PlayerEntry.BIRTH_YEAR,
                PlayerContract.PlayerEntry.BIRTH_MONTH,
                PlayerContract.PlayerEntry.IS_COMING,
                PlayerContract.PlayerEntry.ARCHIVED,
                PlayerContract.PlayerEntry.ATTRIBUTES
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = PlayerContract.PlayerEntry.GRADE + " DESC";

        String where = PlayerContract.PlayerEntry.IS_COMING + " = ? AND " +
                PlayerContract.PlayerEntry.ARCHIVED + " = ? ";
        String[] whereArgs = new String[]{"1", "0"}; // coming and not archived

        Cursor c = db.query(
                PlayerContract.PlayerEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                where,                                // The columns for the WHERE clause
                whereArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        return getPlayers(c);
    }

    @NonNull
    private static ArrayList<Player> getPlayers(Cursor c) {

        ArrayList<Player> players = new ArrayList<>();
        try {
            if (c.moveToFirst()) {
                do {
                    Player p = createPlayerFromCursor(c,
                            PlayerContract.PlayerEntry.NAME,
                            PlayerContract.PlayerEntry.MSG_IDENTIFIER,
                            PlayerContract.PlayerEntry.BIRTH_YEAR,
                            PlayerContract.PlayerEntry.BIRTH_MONTH,
                            PlayerContract.PlayerEntry.GRADE,
                            PlayerContract.PlayerEntry.IS_COMING,
                            PlayerContract.PlayerEntry.ARCHIVED,
                            PlayerContract.PlayerEntry.ATTRIBUTES);
                    players.add(p);
                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

        return players;
    }

    @NonNull
    public static Player createPlayerFromCursor(Cursor c,
                                                String player_name,
                                                String player_msg_identifier,
                                                String year,
                                                String month,
                                                String player_grade,
                                                String is_coming,
                                                String archived,
                                                String attributes) {

        Player p = new Player(c.getString(c.getColumnIndex(player_name)), c.getInt(c.getColumnIndex(player_grade)));
        p.isComing = (is_coming != null) ? c.getInt(c.getColumnIndex(is_coming)) == 1 : true;
        p.archived = (archived != null) ? c.getInt(c.getColumnIndex(archived)) == 1 : false;
        p.mBirthYear = (year != null && c.getColumnIndex(year) > 0) ? c.getInt(c.getColumnIndex(year)) : 0;
        p.mBirthMonth = (month != null && c.getColumnIndex(month) > 0) ? c.getInt(c.getColumnIndex(month)) : 0;
        p.msgDisplayName = (player_msg_identifier != null && c.getColumnIndex(player_msg_identifier) > 0) ? c.getString(c.getColumnIndex(player_msg_identifier)) : "";

        if (attributes != null && c.getColumnIndex(attributes) > 0) {
            String attr = c.getString(c.getColumnIndex(attributes));
            p.isGK = attr.contains(PlayerAttribute.isGK.displayName);
            p.isDefender = attr.contains(PlayerAttribute.isDefender.displayName);
            p.isPlaymaker = attr.contains(PlayerAttribute.isPlaymaker.displayName);
            p.isUnbreakable = attr.contains(PlayerAttribute.isUnbreakable.displayName);
        }
        return p;
    }

    public static void deletePlayer(SQLiteDatabase db, String name) {
        Log.d("DB", "delete player " + name);

        db.delete(PlayerContract.PlayerEntry.TABLE_NAME,
                PlayerContract.PlayerEntry.NAME + " = ? ",
                new String[]{name});
    }

    public static void setPlayerAttributes(SQLiteDatabase db, String name, String attributes) {
        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerEntry.ATTRIBUTES, attributes);

        updatePlayer(db, name, values);
    }

    public static void setPlayerArchive(SQLiteDatabase db, String name, boolean archive) {
        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerEntry.ARCHIVED, archive ? 1 : 0);

        updatePlayer(db, name, values);
    }

    @NonNull
    public static ArrayList<Player> getPlayers(SQLiteDatabase db) {

        String[] projection = {
                PlayerContract.PlayerEntry.ID,
                PlayerContract.PlayerEntry.NAME,
                PlayerContract.PlayerEntry.MSG_IDENTIFIER,
                PlayerContract.PlayerEntry.GRADE,
                PlayerContract.PlayerEntry.BIRTH_YEAR,
                PlayerContract.PlayerEntry.BIRTH_MONTH,
                PlayerContract.PlayerEntry.IS_COMING,
                PlayerContract.PlayerEntry.ARCHIVED,
                PlayerContract.PlayerEntry.ATTRIBUTES
        };

        String sortOrder = PlayerContract.PlayerEntry.NAME + " ASC";

        Cursor c = db.query(
                PlayerContract.PlayerEntry.TABLE_NAME,    // The table to query
                projection,                               // The columns to return
                null,                                    // where
                null,                        // where values
                null,                            // don't group the rows
                null,                             // don't filter by row groups
                sortOrder                                 // The sort order
        );

        return getPlayers(c);
    }

    @NonNull
    public static ArrayList<Player> getPlayers(SQLiteDatabase db, boolean showArchived) {

        String[] projection = {
                PlayerContract.PlayerEntry.ID,
                PlayerContract.PlayerEntry.NAME,
                PlayerContract.PlayerEntry.GRADE,
                PlayerContract.PlayerEntry.BIRTH_YEAR,
                PlayerContract.PlayerEntry.BIRTH_MONTH,
                PlayerContract.PlayerEntry.IS_COMING,
                PlayerContract.PlayerEntry.ARCHIVED,
                PlayerContract.PlayerEntry.ATTRIBUTES
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = PlayerContract.PlayerEntry.GRADE + " DESC";
        String where = PlayerContract.PlayerEntry.ARCHIVED + " = ? ";
        String[] selectionArgs = {showArchived ? "1" : "0"};

        Cursor c = db.query(
                PlayerContract.PlayerEntry.TABLE_NAME,    // The table to query
                projection,                               // The columns to return
                where,                                    // where
                selectionArgs,                        // where values
                null,                            // don't group the rows
                null,                             // don't filter by row groups
                sortOrder                                 // The sort order
        );

        return getPlayers(c);
    }

    @NonNull
    public static ArrayList<Player> getPlayersByIdentifier(SQLiteDatabase db, ArrayList<String> names) {

        String[] projection = {
                PlayerContract.PlayerEntry.ID,
                PlayerContract.PlayerEntry.NAME,
                PlayerContract.PlayerEntry.MSG_IDENTIFIER,
                PlayerContract.PlayerEntry.GRADE,
                PlayerContract.PlayerEntry.BIRTH_YEAR,
                PlayerContract.PlayerEntry.BIRTH_MONTH,
                PlayerContract.PlayerEntry.IS_COMING,
                PlayerContract.PlayerEntry.ARCHIVED,
                PlayerContract.PlayerEntry.ATTRIBUTES
        };

        ArrayList<String> queryName = new ArrayList<>();
        for (String coming : names) {
            queryName.add("\"" + coming + "\"");
        }

        // How you want the results sorted in the resulting Cursor
        String sortOrder = PlayerContract.PlayerEntry.GRADE + " DESC";
        String where = PlayerContract.PlayerEntry.ARCHIVED + " = 0 AND (" +
//                PlayerContract.PlayerEntry.NAME + " IN (" + String.join(",", queryName) + ") OR " +
                PlayerContract.PlayerEntry.MSG_IDENTIFIER + " IN (" + String.join(",", queryName) + "))";
        String[] selectionArgs = null;

        Cursor c = db.query(
                PlayerContract.PlayerEntry.TABLE_NAME,    // The table to query
                projection,                               // The columns to return
                where,                                    // where
                selectionArgs,                        // where values
                null,                            // don't group the rows
                null,                             // don't filter by row groups
                sortOrder                                 // The sort order
        );

        return getPlayers(c);
    }

    public static int setPlayerMsgIdentifier(SQLiteDatabase db, String name, String identifier) {
        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerEntry.MSG_IDENTIFIER, identifier);

        String where = PlayerContract.PlayerEntry.NAME + " = ?  AND " +
                PlayerContract.PlayerEntry.ARCHIVED + " = ? ";
        String[] whereArgs = new String[]{name, "0"};

        return (DbHelper.updateRecord(db, values, where, whereArgs, PlayerContract.PlayerEntry.TABLE_NAME));
    }

    public static void clearPlayerMsgIdentifier(SQLiteDatabase db, String name) {
        setPlayerMsgIdentifier(db, name, "");
    }

    public static boolean insertPlayer(SQLiteDatabase db, Player p) {

        String where = PlayerContract.PlayerEntry.NAME + " = ? ";
        String[] whereArgs = new String[]{p.mName};
        String[] projection = {
                PlayerContract.PlayerEntry.ID};
        Cursor c = db.query(
                PlayerContract.PlayerEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                where,                                // The columns for the WHERE clause
                whereArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        int count = c.getCount();
        Log.d("DB", "Found " + count + " players with " + p.mName);
        c.close();
        if (count > 0) {
            return false;
        }

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerEntry.NAME, p.mName);
        values.put(PlayerContract.PlayerEntry.GRADE, p.mGrade);
        values.put(PlayerContract.PlayerEntry.IS_COMING, p.isComing ? 1 : 0);
        if (p.mBirthYear > 0) values.put(PlayerContract.PlayerEntry.BIRTH_YEAR, p.mBirthYear);
        if (p.mBirthMonth > 0) values.put(PlayerContract.PlayerEntry.BIRTH_MONTH, p.mBirthMonth);
        values.put(PlayerContract.PlayerEntry.ATTRIBUTES, p.getAttributes());
        values.put(PlayerContract.PlayerEntry.ARCHIVED, p.archived ? 1 : 0);

        // Insert the new row, returning the primary key value of the new row
        db.insert(PlayerContract.PlayerEntry.TABLE_NAME,
                null,
                values);

        return true;
    }

    public static void updatePlayerComing(SQLiteDatabase db, String name, boolean coming) {
        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerEntry.IS_COMING, coming ? 1 : 0);

        updatePlayer(db, name, values);
    }

    public static void updatePlayerBirth(SQLiteDatabase db, String name, int year, int month) {
        ContentValues values = new ContentValues();
        if (year > 0) values.put(PlayerContract.PlayerEntry.BIRTH_YEAR, year);
        if (month > 0) values.put(PlayerContract.PlayerEntry.BIRTH_MONTH, month);

        updatePlayer(db, name, values);
    }

    public static void updatePlayerGrade(SQLiteDatabase db, String name, int grade) {
        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerEntry.GRADE, grade);

        updatePlayer(db, name, values);
    }

    public static void updatePlayerName(SQLiteDatabase db, String currentName, String newName) {
        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerEntry.NAME, newName);
        updatePlayer(db, currentName, values);

        String where = PlayerContract.PlayerGameEntry.NAME + " = ? ";
        String[] whereArgs = new String[]{currentName};
        ContentValues values2 = new ContentValues();
        values2.put(PlayerContract.PlayerGameEntry.NAME, newName);
        DbHelper.updateRecord(db, values, where, whereArgs, PlayerContract.PlayerGameEntry.TABLE_NAME);
    }

    private static int updatePlayer(SQLiteDatabase db, String name, ContentValues values) {
        String where = PlayerContract.PlayerEntry.NAME + " = ? ";
        String[] whereArgs = new String[]{name};

        return (DbHelper.updateRecord(db, values, where, whereArgs, PlayerContract.PlayerEntry.TABLE_NAME));
    }

    public static Player getPlayer(SQLiteDatabase db, String name) {

        String[] projection = {
                PlayerContract.PlayerEntry.ID,
                PlayerContract.PlayerEntry.NAME,
                PlayerContract.PlayerEntry.GRADE,
                PlayerContract.PlayerEntry.IS_COMING,
                PlayerContract.PlayerEntry.BIRTH_YEAR,
                PlayerContract.PlayerEntry.BIRTH_MONTH,
                PlayerContract.PlayerEntry.ATTRIBUTES,
                PlayerContract.PlayerEntry.ARCHIVED
        };

        String where = PlayerContract.PlayerEntry.NAME + " = ? ";
        String[] whereArgs = new String[]{name};

        Cursor c = db.query(
                PlayerContract.PlayerEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                where,                                // The columns for the WHERE clause
                whereArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );


        try {
            if (c.moveToFirst()) {
                return createPlayerFromCursor(c,
                        PlayerContract.PlayerEntry.NAME,
                        null,
                        PlayerContract.PlayerEntry.BIRTH_YEAR,
                        PlayerContract.PlayerEntry.BIRTH_MONTH,
                        PlayerContract.PlayerEntry.GRADE,
                        PlayerContract.PlayerEntry.IS_COMING,
                        PlayerContract.PlayerEntry.ARCHIVED,
                        PlayerContract.PlayerEntry.ATTRIBUTES);
            }
        } finally {
            c.close();
        }

        Log.e("TEAMS", "player " + name + " is missing from the DB");
        return null;
    }

    public static void clearAllComing(SQLiteDatabase db) {

        ContentValues values = new ContentValues();
        values.put(PlayerContract.PlayerEntry.IS_COMING, 0);

        DbHelper.updateRecord(db, values, null, null, PlayerContract.PlayerEntry.TABLE_NAME);
    }
}
