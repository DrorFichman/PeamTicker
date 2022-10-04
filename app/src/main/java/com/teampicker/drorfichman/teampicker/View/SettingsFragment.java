package com.teampicker.drorfichman.teampicker.View;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;
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
        setShowGrade();
        setColorScheme();
    }

    private void setColorScheme() {
        Preference color = findPreference(SettingsHelper.SETTING_TEAM_COLOR_SCHEME);
        color.setOnPreferenceChangeListener((preference, newValue) -> {
            LocalNotifications.sendNotification(getContext(), LocalNotifications.SETTING_MODIFIED_ACTION);
            return true;
        });
    }

    private void setShowGrade() {
        Preference showGrade = findPreference(SettingsHelper.SETTING_SHOW_GRADES);
        showGrade.setOnPreferenceChangeListener((preference, newValue) -> {
            LocalNotifications.sendNotification(getContext(), LocalNotifications.SETTING_MODIFIED_ACTION);
            return true;
        });
    }

    private void setTutorialsReset() {
        Preference reset = findPreference(SettingsHelper.SETTING_RESET_TUTORIALS);
        reset.setOnPreferenceClickListener(preference -> {
            TutorialManager.clearTutorialPreferences(getContext());
            LocalNotifications.sendNotification(getContext(), LocalNotifications.SETTING_MODIFIED_ACTION);
            Toast.makeText(getContext(), "Tutorial reset", Toast.LENGTH_SHORT).show();
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

    }

    private void setDivisionAttemptsPreference() {
        EditTextPreference attemptsPref = findPreference(SettingsHelper.SETTING_DIVIDE_ATTEMPTS);
        attemptsPref.setOnBindEditTextListener(editText ->
        {
            editText.setText(String.valueOf(SettingsHelper.getDivideAttemptsCount(getContext())));
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setSingleLine();
        });
    }
}
