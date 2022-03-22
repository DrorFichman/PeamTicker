package com.teampicker.drorfichman.teampicker.tools;

import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.ToDo;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.teampicker.drorfichman.teampicker.Adapter.TutorialStepsAdapter;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialAttendance;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialCloud;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialGameHistory;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialNewPlayer;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialPickTeams;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialSaveResults;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialShuffleAI;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialStartPickTeams;
import com.teampicker.drorfichman.teampicker.tools.tutorials.AbstractTutorialStep;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialTeamAnalysis;

import java.util.ArrayList;
import java.util.Collections;

public class TutorialManager {

    public static TutorialDisplayState current;

    public static void setDialogDismissed() {
        current = TutorialManager.TutorialDisplayState.NotDisplayed;
    }

    public static void showTutorialDialog(Context ctx, String prefKey, boolean forceShow, String title, String message, int location) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setIcon(R.drawable.information);

        alertDialogBuilder.setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setOnCancelListener(dialogInterface -> TutorialManager.setDialogDismissed())
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    TutorialManager.setDialogDismissed();
                });

        if (!forceShow)
            alertDialogBuilder.setNeutralButton("Don't show again", (dialogInterface, i) -> {
                PreferenceHelper.setSharedPreferenceString(ctx, prefKey, "1");
                TutorialManager.setDialogDismissed();
            });

        AlertDialog dialog = alertDialogBuilder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setGravity(forceShow ? Gravity.CENTER : location);
        dialog.show();
    }

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

        public AbstractTutorialStep dialog;

        Tutorials(AbstractTutorialStep i) {
            dialog = i;
        }
    }

    public enum TutorialStepStatus {
        ToDo,
        Done,
        NotApplicable
    }

    public enum TutorialDisplayState {
        Displayed,
        NotDisplayed,
        AnotherDialogDisplayed
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

    public static void displayTutorialFlow(Context ctx, DialogInterface.OnCancelListener onCancel) {
        Dialog dialog = new Dialog(ctx);
        dialog.setContentView(R.layout.layout_tutorial_dialog_view);

        ListView tutorials = dialog.findViewById(R.id.tutorial_list);
        ArrayList<Tutorials> ts = new ArrayList<>();
        Collections.addAll(ts, Tutorials.values());

        TutorialStepsAdapter playersAdapter = new TutorialStepsAdapter(ctx, ts);
        tutorials.setAdapter(playersAdapter);

        dialog.setOnCancelListener(onCancel);
        dialog.show();
    }

    public static TutorialDisplayState displayTutorialStep(Context ctx, Tutorials step, boolean forceShow) {
        if (current == TutorialDisplayState.AnotherDialogDisplayed) {
            return TutorialDisplayState.AnotherDialogDisplayed;
        }

        AbstractTutorialStep dialog = step.dialog;
        if (forceShow || (dialog.shouldBeDisplayed(ctx) == ToDo && !dialog.isDialogDismissedByUser(ctx))) {
            dialog.display(ctx, forceShow);
            current = TutorialDisplayState.AnotherDialogDisplayed;
            return TutorialDisplayState.AnotherDialogDisplayed;
        } else {
            return TutorialDisplayState.NotDisplayed;
        }
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
            PreferenceHelper.getSharedPreference(ctx).edit().remove(a.dialog.prefKey()).commit();
        }
    }

    public static int getProgress(Context context) {
        int total = Tutorials.values().length;
        int completed = 0;

        for (Tutorials t : Tutorials.values()) {
            if (t.dialog.shouldBeDisplayed(context) == Done) {
                completed += 1;
            }
        }

        return completed * 100 / total;
    }
}
