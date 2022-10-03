package com.teampicker.drorfichman.teampicker.tools;

import android.content.Context;
import android.util.Log;

import com.teampicker.drorfichman.teampicker.Controller.TeamDivision.DivisionWeight;

import androidx.preference.PreferenceManager;

public class SettingsHelper {

    public static final String SETTING_DIVIDE_ATTEMPTS = "divide_attempts";
    public static final String SETTING_DIVIDE_GRADE = "divide_grade_percentage";
    public static final String SETTING_TEAM_COLOR_SCHEME = "teams_color_scheme";
    public static final String SETTING_RESET_TUTORIALS = "setting_clear_tutorial";
    public static final String SETTING_SHOW_GRADES = "show_grades";
    public static final String SETTING_AUTO_SYNC_CLOUD = "auto_sync_cloud";

    private static int getPreferenceValue(Context ctx, String preferenceKey, int defaultValue) {
        try {
            String value = PreferenceManager.getDefaultSharedPreferences(ctx).getString(preferenceKey, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (Exception e) {
            Log.e(preferenceKey, "failed getting " + preferenceKey, e);
        }
        return defaultValue;
    }

    private static String getPreferenceValue(Context ctx, String preferenceKey, String defaultValue) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(preferenceKey, String.valueOf(defaultValue));
        } catch (Exception e) {
            Log.e(preferenceKey, "failed getting " + preferenceKey, e);
        }
        return defaultValue;
    }

    public static int getDivideAttemptsCount(Context ctx) {
        int divideAttempts = getPreferenceValue(ctx, SETTING_DIVIDE_ATTEMPTS, 50);
        return MathTools.getLimitedValue(divideAttempts, 1, 200);
    }

    public static DivisionWeight getDivisionWeight(Context ctx) {
        int gradeWeight = getPreferenceValue(ctx, SETTING_DIVIDE_GRADE, 20);
        int grade = MathTools.getLimitedValue(gradeWeight, 0, 100);
        int chemistry = (100 - grade) / 2; // default 40
        int stdDev = 100 - grade - chemistry; // default 40
        return new DivisionWeight(grade, chemistry, stdDev);
    }

    public static String getColorScheme(Context ctx) {
        return getPreferenceValue(ctx, SETTING_TEAM_COLOR_SCHEME,
                ctx.getString(ColorHelper.ColorScheme.OrangeBlue.stringRes));
    }

    public static boolean getShowGrades(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(SETTING_SHOW_GRADES, true);
    }

    public static boolean getAutoSyncCloud(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(SETTING_AUTO_SYNC_CLOUD, false);
    }
}
