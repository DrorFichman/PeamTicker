package com.teampicker.drorfichman.teampicker.Data;

import java.util.ArrayList;

import androidx.annotation.NonNull;

/**
 * Created by drorfichman on 7/30/16.
 */
public final class PlayerContract {
    public PlayerContract() {
    }

    @NonNull
    public static ArrayList<String> getTables() {
        ArrayList<String> tables = new ArrayList<>(3);
        tables.add(PlayerContract.PlayerEntry.TABLE_NAME);
        tables.add(PlayerContract.GameEntry.TABLE_NAME);
        tables.add(PlayerContract.PlayerGameEntry.TABLE_NAME);
        return tables;
    }

    public static abstract class PlayerEntry {
        public static final String TABLE_NAME = "player";
        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String GRADE = "grade";
        public static final String IS_COMING = "is_coming";
        public static final String BIRTH_YEAR = "birth_year";
        public static final String BIRTH_MONTH = "birth_month";
        public static final String ATTRIBUTES = "attributes";
        public static final String ARCHIVED = "archived";
        public static final String MSG_IDENTIFIER = "message_identifier";
    }

    public static abstract class PlayerGameEntry {
        public static final String TABLE_NAME = "player_game";
        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String GAME = "game";
        public static final String DATE = "date"; // "yyyy-MM-dd"
        public static final String PLAYER_GRADE = "player_grade";
        public static final String PLAYER_AGE = "age";
        public static final String GAME_GRADE = "game_grade";
        public static final String TEAM = "team";
        public static final String GOALS = "goals";
        public static final String ASSISTS = "assists";
        public static final String PLAYER_RESULT = "result";
        public static final String DID_WIN = "did_win";
        public static final String ATTRIBUTES = "attributes";
    }

    public static abstract class GameEntry {
        public static final String TABLE_NAME = "game";
        public static final String ID = "_id";
        public static final String GAME = "game_index";
        public static final String DATE = "date"; // "yyyy-MM-dd"
        public static final String TEAM1_SCORE = "team_one_score";
        public static final String TEAM2_SCORE = "team_two_score";
        public static final String TEAM_RESULT = "result";
    }
}

