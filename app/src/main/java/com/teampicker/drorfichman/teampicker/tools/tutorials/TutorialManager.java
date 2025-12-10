package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.ToDo;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.Adapter.TutorialStepsAdapter;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;

import java.util.ArrayList;
import java.util.Collections;

public class TutorialManager {

    public enum Tutorials {
        players(TutorialNewPlayer.instance),
        attendance(TutorialAttendance.instance),
        start_pick_teams(TutorialStartPickTeams.instance),
        pick_teams(TutorialPickTeams.instance),
        save_results(TutorialSaveResults.instance),
        game_history(TutorialGameHistory.instance),
        cloud(TutorialCloud.instance),
        team_shuffle_stats(TutorialShuffleAI.instance),
        team_analysis(TutorialTeamAnalysis.instance);

        public AbstractTutorialStep step;

        Tutorials(AbstractTutorialStep i) {
            step = i;
        }
    }

    public enum TutorialUserAction {
        clicked_teams(PreferenceHelper.pref_tutorial_clicked_teams),
        clicked_shuffle(PreferenceHelper.pref_tutorial_clicked_shuffle),
        clicked_game_in_history(PreferenceHelper.pref_tutorial_clicked_game_history),
        click_sync_to_cloud(PreferenceHelper.pref_tutorial_clicked_sync),
        clicked_analysis(PreferenceHelper.pref_tutorial_clicked_analysis),
        clicked_shuffle_stats(PreferenceHelper.pref_tutorial_clicked_shuffle_stats);

        private final String prefKey;

        TutorialUserAction(String pref) {
            prefKey = pref;
        }
    }

    public enum TutorialStepStatus {
        ToDo,
        Done,
        NotApplicable
    }

    private static boolean isSpotlightDisplayed = false;

    /**
     * Shows a spotlight hint on a toolbar menu item using TapTargetView
     */
    public static void showSpotlightOnToolbarMenuItem(Activity activity, Toolbar toolbar, 
                                                       @IdRes int menuItemId, String prefKey,
                                                       boolean forceShow, String title, String description) {
        if (toolbar == null) {
            return;
        }

        isSpotlightDisplayed = true;

        TapTarget tapTarget = TapTarget.forToolbarMenuItem(toolbar, menuItemId, title, description)
                .outerCircleColor(R.color.colorPrimary)
                .outerCircleAlpha(0.9f)
                .targetCircleColor(android.R.color.white)
                .titleTextSize(20)
                .titleTextColor(android.R.color.white)
                .descriptionTextSize(14)
                .descriptionTextColor(android.R.color.white)
                .textColor(android.R.color.white)
                .dimColor(android.R.color.black)
                .drawShadow(true)
                .cancelable(true)
                .tintTarget(true)
                .targetRadius(40);

        TapTargetView.showFor(activity, tapTarget, new TapTargetView.Listener() {
            @Override
            public void onTargetClick(TapTargetView view) {
                super.onTargetClick(view);
                if (!forceShow) {
                    PreferenceHelper.setSharedPreferenceString(activity, prefKey, "1");
                }
                isSpotlightDisplayed = false;
            }

            @Override
            public void onTargetLongClick(TapTargetView view) {
                dismissAllTutorials(activity, true);
                view.dismiss(true);
                isSpotlightDisplayed = false;
            }

            @Override
            public void onOuterCircleClick(TapTargetView view) {
                view.dismiss(false);
                if (!forceShow) {
                    PreferenceHelper.setSharedPreferenceString(activity, prefKey, "1");
                }
                isSpotlightDisplayed = false;
            }

            @Override
            public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                isSpotlightDisplayed = false;
            }
        });
    }

    /**
     * Shows a spotlight hint on the target view using TapTargetView
     */
    public static void showSpotlight(Activity activity, View targetView, String prefKey,
                                     boolean forceShow, String title, String description) {
        if (targetView == null || !targetView.isShown()) {
            return;
        }

        isSpotlightDisplayed = true;

        TapTarget tapTarget = TapTarget.forView(targetView, title, description)
                .outerCircleColor(R.color.colorPrimary)
                .outerCircleAlpha(0.9f)
                .targetCircleColor(android.R.color.white)
                .titleTextSize(20)
                .titleTextColor(android.R.color.white)
                .descriptionTextSize(14)
                .descriptionTextColor(android.R.color.white)
                .textColor(android.R.color.white)
                .dimColor(android.R.color.black)
                .drawShadow(true)
                .cancelable(true)
                .tintTarget(false)
                .transparentTarget(true)
                .targetRadius(60);

        TapTargetView.showFor(activity, tapTarget, new TapTargetView.Listener() {
            @Override
            public void onTargetClick(TapTargetView view) {
                super.onTargetClick(view);
                if (!forceShow) {
                    PreferenceHelper.setSharedPreferenceString(activity, prefKey, "1");
                }
                isSpotlightDisplayed = false;
            }

            @Override
            public void onTargetLongClick(TapTargetView view) {
                dismissAllTutorials(activity, true);
                view.dismiss(true);
                isSpotlightDisplayed = false;
            }

            @Override
            public void onOuterCircleClick(TapTargetView view) {
                view.dismiss(false);
                if (!forceShow) {
                    PreferenceHelper.setSharedPreferenceString(activity, prefKey, "1");
                }
                isSpotlightDisplayed = false;
            }

            @Override
            public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                isSpotlightDisplayed = false;
            }
        });
    }

    public static void displayTutorialFlow(Context ctx, DialogInterface.OnCancelListener onCancel) {
        Dialog dialog = new Dialog(ctx);
        dialog.setContentView(R.layout.layout_tutorial_dialog_view);

        ListView tutorials = dialog.findViewById(R.id.tutorial_list);
        ArrayList<Tutorials> ts = new ArrayList<>();
        Collections.addAll(ts, Tutorials.values());

        TutorialStepsAdapter playersAdapter = new TutorialStepsAdapter(ctx, ts);
        tutorials.setAdapter(playersAdapter);

        CheckBox dismiss = ((CheckBox) dialog.findViewById(R.id.tutorials_dismiss_all));
        dismiss.setChecked(TutorialManager.isSkipAllTutorials(ctx));
        dismiss.setOnCheckedChangeListener((compoundButton, checked) -> TutorialManager.dismissAllTutorials(ctx, checked));

        dialog.setOnCancelListener(onCancel);
        dialog.show();
    }

    /**
     * Displays a tutorial step spotlight on the provided target view
     * @return true if the spotlight was displayed, false otherwise
     */
    public static boolean displayTutorialStep(Activity activity, Tutorials tutorial, View targetView, boolean forceShow) {
        if (isSpotlightDisplayed) {
            return false;
        }

        AbstractTutorialStep step = tutorial.step;
        TutorialStepStatus status = step.shouldBeDisplayed(activity);
        boolean isDismissed = step.isDialogDismissedByUser(activity);
        boolean isSkipAll = isSkipAllTutorials(activity);

        if (forceShow || (status == ToDo && !isDismissed && !isSkipAll)) {
            step.display(activity, targetView, forceShow);
            return true;
        }
        return false;
    }

    /**
     * Displays a tutorial step spotlight on a toolbar menu item
     * @return true if the spotlight was displayed, false otherwise
     */
    public static boolean displayTutorialStepOnMenuItem(Activity activity, Tutorials tutorial, 
                                                         Toolbar toolbar, @IdRes int menuItemId, 
                                                         boolean forceShow) {
        if (isSpotlightDisplayed || toolbar == null) {
            return false;
        }

        AbstractTutorialStep step = tutorial.step;
        TutorialStepStatus status = step.shouldBeDisplayed(activity);
        boolean isDismissed = step.isDialogDismissedByUser(activity);
        boolean isSkipAll = isSkipAllTutorials(activity);

        if (forceShow || (status == ToDo && !isDismissed && !isSkipAll)) {
            String title = step.name();
            String description = getTutorialDescription(activity, tutorial);
            showSpotlightOnToolbarMenuItem(activity, toolbar, menuItemId, step.prefKey(), 
                    forceShow, title, description);
            return true;
        }
        return false;
    }

    /**
     * Check if a spotlight is currently displayed
     */
    public static boolean isSpotlightDisplayed() {
        return isSpotlightDisplayed;
    }

    /**
     * Shows a simple dialog with tutorial information (fallback when no target view is available)
     * Used primarily from the "Getting Started" menu
     */
    public static void showTutorialInfoDialog(Context ctx, String title, String description) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setIcon(R.drawable.information)
                .setTitle(title)
                .setMessage(description)
                .setCancelable(true)
                .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    /**
     * Display a tutorial step - used from the Getting Started menu
     * Shows info dialog since there's no target view in menu context
     */
    public static void displayTutorialFromMenu(Context ctx, Tutorials tutorial) {
        AbstractTutorialStep step = tutorial.step;
        showTutorialInfoDialog(ctx, step.name(), getTutorialDescription(ctx, tutorial));
    }

    /**
     * Displays a tutorial step as a simple dialog (for tutorials without good target views)
     * @return true if the dialog was displayed, false otherwise
     */
    public static boolean displayTutorialStepAsDialog(Activity activity, Tutorials tutorial, boolean forceShow) {
        if (isSpotlightDisplayed) {
            return false;
        }

        AbstractTutorialStep step = tutorial.step;
        TutorialStepStatus status = step.shouldBeDisplayed(activity);
        boolean isDismissed = step.isDialogDismissedByUser(activity);
        boolean isSkipAll = isSkipAllTutorials(activity);

        if (forceShow || (status == ToDo && !isDismissed && !isSkipAll)) {
            String title = step.name();
            String description = getTutorialDescription(activity, tutorial);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setIcon(R.drawable.information)
                    .setTitle(title)
                    .setMessage(description)
                    .setCancelable(true)
                    .setPositiveButton("Got it", (dialog, which) -> {
                        if (!forceShow) {
                            PreferenceHelper.setSharedPreferenceString(activity, step.prefKey(), "1");
                        }
                        dialog.dismiss();
                    });
            builder.create().show();
            return true;
        }
        return false;
    }

    private static String getTutorialDescription(Context ctx, Tutorials tutorial) {
        switch (tutorial) {
            case players:
                return ctx.getString(R.string.tutorial_create_player_message);
            case attendance:
                return ctx.getString(R.string.tutorial_attendance_message);
            case start_pick_teams:
                return ctx.getString(R.string.tutorial_start_pick_teams_message);
            case pick_teams:
                return ctx.getString(R.string.tutorial_pick_teams_message);
            case save_results:
                return ctx.getString(R.string.tutorial_save_results_message);
            case game_history:
                return ctx.getString(R.string.tutorial_game_history_message);
            case cloud:
                return ctx.getString(R.string.tutorial_cloud_message);
            case team_shuffle_stats:
                return ctx.getString(R.string.tutorial_team_shuffle_stats_message);
            case team_analysis:
                return ctx.getString(R.string.tutorial_team_analysis_message);
            default:
                return "";
        }
    }

    public static void dismissAllTutorials(Context ctx, boolean dismiss) {
        PreferenceHelper.setSharedPreferenceString(ctx, PreferenceHelper.pref_skip_all_tutorial,
                dismiss ? "1" : null);
        Event.logEvent(FirebaseAnalytics.getInstance(ctx), EventType.tutorial_dismissed);
    }

    public static boolean isSkipAllTutorials(Context ctx) {
        // Check new settings preference first (default shared prefs)
        // If it exists, use its value (true = show hints, false = skip)
        android.content.SharedPreferences defaultPrefs = 
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx);
        if (defaultPrefs.contains(PreferenceHelper.pref_skip_all_tutorial)) {
            // Switch is "Show Hints" - true means show, false means skip
            return !defaultPrefs.getBoolean(PreferenceHelper.pref_skip_all_tutorial, true);
        }
        
        // Fall back to old custom preference (for backward compatibility)
        // Old system: presence of key means skip tutorials
        return PreferenceHelper.getSharedPreference(ctx).contains(PreferenceHelper.pref_skip_all_tutorial);
    }

    public static void userActionTaken(Context ctx, TutorialUserAction action) {
        PreferenceHelper.setSharedPreferenceString(ctx, action.prefKey, "1");
    }

    public static boolean isActionTakenByUser(Context ctx, TutorialUserAction action) {
        return PreferenceHelper.getSharedPreference(ctx).contains(action.prefKey);
    }

    public static void clearTutorialPreferences(Context ctx) {
        for (TutorialUserAction a : TutorialUserAction.values()) {
            PreferenceHelper.getSharedPreference(ctx).edit().remove(a.prefKey).commit();
        }
        for (Tutorials a : Tutorials.values()) {
            PreferenceHelper.getSharedPreference(ctx).edit().remove(a.step.prefKey()).commit();
        }
        PreferenceHelper.getSharedPreference(ctx).edit().remove(PreferenceHelper.pref_skip_all_tutorial).commit();
    }
}
