package com.teampicker.drorfichman.teampicker.tools;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by drorfichman on 7/29/16.
 */
public class PreferenceHelper {
    private final static String PREF_FILE = "PLAYERS";

    public static String pref_shuffle = "shuffle_strategy";
    
    public static String pref_weather_start_hour = "weather_start_hour";
    public static String pref_weather_start_minute = "weather_start_minute";
    public static String pref_weather_end_hour = "weather_end_hour";
    public static String pref_weather_end_minute = "weather_end_minute";
    public static String pref_weather_day_of_week = "weather_day_of_week";
    public static String pref_weather_location_name = "weather_location_name";
    public static String pref_weather_location_lat = "weather_location_lat";
    public static String pref_weather_location_lng = "weather_location_lng";

    public static String pref_skip_tutorial_players = "pref_skip_tutorial_players";
    public static String pref_skip_tutorial_attendance = "pref_skip_tutorial_attendance";
    public static String pref_skip_tutorial_pick = "pref_skip_tutorial_pick";
    public static String pref_skip_tutorial_start_teams = "pref_skip_tutorial_start_teams";
    public static String pref_skip_tutorial_save = "pref_skip_tutorial_save";
    public static String pref_skip_tutorial_history = "pref_skip_tutorial_history";
    public static String pref_skip_tutorial_cloud = "pref_skip_tutorial_cloud";
    public static String pref_skip_tutorial_shuffle_stats = "pref_skip_tutorial_shuffle_stats";
    public static String pref_skip_tutorial_analysis = "pref_skip_tutorial_analysis";
    public static String pref_skip_all_tutorial = "pref_skip_all_tutorial";

    public static String pref_tutorial_clicked_teams = "pref_tutorial_clicked_teams";
    public static String pref_tutorial_clicked_shuffle = "pref_tutorial_clicked_shuffle";
    public static String pref_tutorial_clicked_game_history = "pref_tutorial_clicked_game_history";
    public static String pref_tutorial_clicked_sync = "pref_tutorial_clicked_sync";
    public static String pref_tutorial_clicked_analysis = "pref_tutorial_clicked_analysis";
    public static String pref_tutorial_clicked_shuffle_stats = "pref_tutorial_clicked_shuffle_stats";

    public static String pref_last_synced_game_id = "pref_last_synced_game_id";

    /**
     * Get the last synced game ID
     * @return the last synced game ID, or 0 if never synced
     */
    public static int getLastSyncedGameId(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        return settings.getInt(pref_last_synced_game_id, 0);
    }

    /**
     * Set the last synced game ID
     * @param gameId the game ID that was last synced
     */
    public static void setLastSyncedGameId(Context context, int gameId) {
        setSharedPreferenceInt(context, pref_last_synced_game_id, gameId);
    }

    /**
     * Set a string shared preference
     * @param key - Key to set shared preference
     * @param value - Value for the key
     */
    public static void setSharedPreferenceString(Context context, String key, String value){
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Set a integer shared preference
     * @param key - Key to set shared preference
     * @param value - Value for the key
     */
    static void setSharedPreferenceInt(Context context, String key, int value){
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Set a Boolean shared preference
     * @param key - Key to set shared preference
     * @param value - Value for the key
     */
    static void setSharedPreferenceBoolean(Context context, String key, boolean value){
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static SharedPreferences getSharedPreference(Context context){
        return context.getSharedPreferences(PREF_FILE, 0);
    }
}