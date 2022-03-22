package com.teampicker.drorfichman.teampicker.tools;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by drorfichman on 7/29/16.
 */
public class PreferenceHelper {
    private final static String PREF_FILE = "PLAYERS";

    public static String pref_shuffle = "shuffle_strategy";

    public static String pref_skip_tutorial_players = "pref_skip_tutorial_players";
    public static String pref_skip_tutorial_attendance = "pref_skip_tutorial_attendance";
    public static String pref_skip_tutorial_pick = "pref_skip_tutorial_pick";
    public static String pref_skip_tutorial_start_teams = "pref_skip_tutorial_start_teams";
    public static String pref_skip_tutorial_save = "pref_skip_tutorial_save";
    public static String pref_skip_tutorial_history = "pref_skip_tutorial_history";
    public static String pref_skip_tutorial_cloud = "pref_skip_tutorial_cloud";
    public static String pref_skip_tutorial_shuffle_stats = "pref_skip_tutorial_shuffle_stats";
    public static String pref_skip_tutorial_analysis = "pref_skip_tutorial_analysis";

    public static String pref_tutorial_clicked_teams = "pref_tutorial_clicked_teams";
    public static String pref_tutorial_clicked_shuffle = "pref_tutorial_clicked_shuffle";
    public static String pref_tutorial_clicked_game_history = "pref_tutorial_clicked_game_history";
    public static String pref_tutorial_clicked_sync = "pref_tutorial_clicked_sync";
    public static String pref_tutorial_clicked_analysis = "pref_tutorial_clicked_analysis";
    public static String pref_tutorial_clicked_shuffle_stats = "pref_tutorial_clicked_shuffle_stats";

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