package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.ToDo;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;

public class TutorialAttendance extends AbstractTutorialStep {

    static TutorialAttendance instance = new TutorialAttendance();

    @Override
    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if no games and no current coming player
        boolean hasGamesForAttendance = DbHelper.getGames(ctx, 1).size() > 0;
        boolean hasPlayersForAttendance = DbHelper.getPlayers(ctx, 0, false).size() > 0;
        boolean hasComingPlayersForAttendance = DbHelper.getComingPlayers(ctx, 0).size() > 0;
        
        if (!hasPlayersForAttendance) {
            return TutorialManager.TutorialStepStatus.NotApplicable;
        } else if (!hasGamesForAttendance && !hasComingPlayersForAttendance) {
            return ToDo;
        } else {
            return Done;
        }
    }

    @Override
    public void display(Activity activity, View targetView, boolean forceShow) {
        TutorialManager.showSpotlight(activity, targetView, prefKey(),
                forceShow, activity.getString(R.string.tutorial_attendance_title),
                activity.getString(R.string.tutorial_attendance_message));
    }

    @Override
    public String prefKey() {
        return PreferenceHelper.pref_skip_tutorial_attendance;
    }

    @Override
    public String name() {
        return "Attendance";
    }
}
