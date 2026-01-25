package com.teampicker.drorfichman.teampicker.View;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;
import com.teampicker.drorfichman.teampicker.tools.analytics.ParameterType;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

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
        android.content.SharedPreferences defaultPrefs = 
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
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
        gradeWeight.setOnBindEditTextListener(editText ->
        {
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
        attemptsPref.setOnBindEditTextListener(editText ->
        {
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
}
