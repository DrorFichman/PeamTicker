package com.teampicker.drorfichman.teampicker.View;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.DbAsync;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;
import com.teampicker.drorfichman.teampicker.tools.TestDataGenerator;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;
import com.teampicker.drorfichman.teampicker.tools.analytics.ParameterType;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        setDivisionAttemptsPreference();
        setDivisionGradePercentage();
        setTutorialsReset();
        setShowHints();
        setShowGrade();
        setColorScheme();
        setTeamsCount();
        setAutoResetComingPlayers();
        setAdminGenerateTestData();
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseHelper.executePostConfiguration(requireContext(), false, this::updateAdminVisibility);
    }

    private void updateAdminVisibility() {
        if (getActivity() == null) return;
        PreferenceCategory adminCategory = findPreference("admin_category");
        if (adminCategory != null) {
            adminCategory.setVisible(FirebaseHelper.isAdmin());
        }
    }

    private void setAdminGenerateTestData() {
        Preference genPref = findPreference("admin_generate_test_data");
        if (genPref == null) return;
        genPref.setOnPreferenceClickListener(preference -> {
            showGenerateTestDataDialog();
            return true;
        });
    }

    private void showGenerateTestDataDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_generate_test_data, null);
        EditText playersEdit = view.findViewById(R.id.players_count);
        EditText gamesEdit = view.findViewById(R.id.games_count);
        playersEdit.setText(String.valueOf(30));
        gamesEdit.setText(String.valueOf(50));

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_generate_test_data_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int players = parsePositiveInt(playersEdit.getText(), 30);
                    int games = parsePositiveInt(gamesEdit.getText(), 50);
                    runGenerateTestData(players, games);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private int parsePositiveInt(CharSequence s, int defaultVal) {
        if (TextUtils.isEmpty(s)) return defaultVal;
        try {
            int v = Integer.parseInt(s.toString().trim());
            return v > 0 ? v : defaultVal;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private void runGenerateTestData(int playerCount, int gameCount) {
        DbAsync.run(
                () -> {
                    TestDataGenerator gen = new TestDataGenerator();
                    return gen.generateAll(requireContext(), playerCount, gameCount);
                },
                result -> {
                    if (getActivity() == null) return;
                    if (result != null) {
                        LocalNotifications.sendNotification(requireContext(), LocalNotifications.PULL_DATA_ACTION);
                        Toast.makeText(requireContext(), R.string.toast_test_data_generated, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.toast_test_data_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setTeamsCount() {

    }

    private void setColorScheme() {
        Preference color = findPreference(SettingsHelper.SETTING_TEAM_COLOR_SCHEME);
        color.setOnPreferenceChangeListener((preference, newValue) -> {
            LocalNotifications.sendNotification(getContext(), LocalNotifications.SETTING_MODIFIED_ACTION);
            Event.logEvent(FirebaseAnalytics.getInstance(getActivity()), EventType.settings_changed_color);
            return true;
        });
    }

    private void setShowGrade() {
        Preference showGrade = findPreference(SettingsHelper.SETTING_SHOW_GRADES);
        showGrade.setOnPreferenceChangeListener((preference, newValue) -> {
            LocalNotifications.sendNotification(getContext(), LocalNotifications.SETTING_MODIFIED_ACTION);
            Event.logEvent(FirebaseAnalytics.getInstance(getActivity()), EventType.settings_changed_grades);
            return true;
        });
    }

    private void setTutorialsReset() {
        Preference reset = findPreference(SettingsHelper.SETTING_RESET_TUTORIALS);
        reset.setOnPreferenceClickListener(preference -> {
            TutorialManager.clearTutorialPreferences(getContext());
            LocalNotifications.sendNotification(getContext(), LocalNotifications.SETTING_MODIFIED_ACTION);
            Toast.makeText(getContext(), getString(R.string.toast_success_tutorial_reset), Toast.LENGTH_SHORT).show();
            Event.logEvent(FirebaseAnalytics.getInstance(getActivity()), EventType.settings_changed_tutorial_reset);
            return true;
        });
    }

    private void setShowHints() {
        androidx.preference.SwitchPreferenceCompat showHints = findPreference(SettingsHelper.SETTING_SHOW_HINTS);

        // Initialize switch based on old preference if new preference not yet set
        android.content.SharedPreferences defaultPrefs = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(getContext());
        if (!defaultPrefs.contains(SettingsHelper.SETTING_SHOW_HINTS)) {
            // Check old custom preference - if key exists, user had dismissed tutorials
            boolean oldSkipAll = com.teampicker.drorfichman.teampicker.tools.PreferenceHelper
                    .getSharedPreference(getContext())
                    .contains(com.teampicker.drorfichman.teampicker.tools.PreferenceHelper.pref_skip_all_tutorial);
            // Switch is "Show Hints" - set to false if user had previously dismissed
            showHints.setChecked(!oldSkipAll);
        }

        showHints.setOnPreferenceChangeListener((preference, newValue) -> {
            Event.logEvent(FirebaseAnalytics.getInstance(getActivity()), EventType.settings_changed_show_hints);
            return true;
        });
    }

    private void setDivisionGradePercentage() {
        EditTextPreference gradeWeight = findPreference(SettingsHelper.SETTING_DIVIDE_GRADE);
        gradeWeight.setOnBindEditTextListener(editText -> {
            editText.setText(String.valueOf(SettingsHelper.getDivisionWeight(getContext()).gradeDisplay()));
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setSingleLine();
        });

        gradeWeight.setOnPreferenceChangeListener((preference, newValue) -> {
            Event event = new Event(EventType.settings_changed_division_percentage);
            event.set(ParameterType.percentage, String.valueOf(newValue));
            event.log(FirebaseAnalytics.getInstance(getContext()));
            return true;
        });
    }

    private void setDivisionAttemptsPreference() {
        EditTextPreference attemptsPref = findPreference(SettingsHelper.SETTING_DIVIDE_ATTEMPTS);
        attemptsPref.setOnBindEditTextListener(editText -> {
            editText.setText(String.valueOf(SettingsHelper.getDivideAttemptsCount(getContext())));
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setSingleLine();
        });

        attemptsPref.setOnPreferenceChangeListener((preference, newValue) -> {
            Event event = new Event(EventType.settings_changed_division_attempts);
            event.set(ParameterType.percentage, String.valueOf(newValue));
            event.log(FirebaseAnalytics.getInstance(getContext()));
            return true;
        });
    }

    private void setAutoResetComingPlayers() {
        androidx.preference.SwitchPreferenceCompat autoReset = findPreference(SettingsHelper.SETTING_AUTO_RESET_COMING);
        autoReset.setOnPreferenceChangeListener((preference, newValue) -> {
            Event.logEvent(FirebaseAnalytics.getInstance(getActivity()), EventType.settings_changed_auto_reset_coming);
            return true;
        });
    }
}
