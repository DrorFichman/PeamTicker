package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;

import java.util.Set;

/**
 * Dialog to let user choose between 2 teams or 3 teams
 */
public class TeamCountSelectionDialog {

    public interface OnTeamCountSelectedListener {
        void onTeamCountSelected(int teamCount);
    }

    public static void show(Context context, OnTeamCountSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.dialog_team_count_title);
        builder.setMessage(R.string.dialog_team_count_message);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.dialog_option_2_teams, (dialog, which) -> {
            saveTeamCountPreference(context, 2);
            markDialogShown(context);
            if (listener != null) {
                listener.onTeamCountSelected(2);
            }
            dialog.dismiss();
        });

        builder.setNegativeButton(R.string.dialog_option_3_teams, (dialog, which) -> {
            saveTeamCountPreference(context, 3);
            markDialogShown(context);
            if (listener != null) {
                listener.onTeamCountSelected(3);
            }
            dialog.dismiss();
        });

        builder.create().show();
    }

    public static boolean hasDialogBeenShown(Context context) {
        return PreferenceHelper.getSharedPreference(context)
                .getBoolean(PreferenceHelper.pref_team_count_dialog_shown, false);
    }

    private static void saveTeamCountPreference(Context context, int teamCount) {
        PreferenceHelper.getSharedPreference(context).edit()
                .putInt(PreferenceHelper.pref_team_count, teamCount)
                .apply();
        
        // Also update the settings preference
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(SettingsHelper.SETTING_TEAM_COUNT, String.valueOf(teamCount))
                .apply();
    }

    private static void markDialogShown(Context context) {
        PreferenceHelper.getSharedPreference(context).edit()
                .putBoolean(PreferenceHelper.pref_team_count_dialog_shown, true)
                .apply();
    }
}

