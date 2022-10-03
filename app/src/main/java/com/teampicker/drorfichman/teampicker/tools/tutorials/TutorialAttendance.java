package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.ToDo;

import android.content.Context;
import android.view.Gravity;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;

public class TutorialAttendance extends AbstractTutorialStep {

    static TutorialAttendance instance = new TutorialAttendance();

    @Override
    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if no games and no current coming player
        boolean hasGamesForAttendance = DbHelper.getGames(ctx, 1).size() > 0;
        boolean hasPlayersForAttendance = DbHelper.getPlayers(ctx, 0, false).size() > 1;
        boolean hasComingPlayersForAttendance = DbHelper.getComingPlayers(ctx, 0).size() > 0;
        if (!hasPlayersForAttendance) return TutorialManager.TutorialStepStatus.NotApplicable;
        else if (!hasGamesForAttendance && !hasComingPlayersForAttendance) return ToDo;
        else return Done;
    }

    @Override
    public void display(Context ctx, boolean forceShow) {
        TutorialManager.showTutorialDialog(ctx, prefKey(),
                forceShow, ctx.getString(R.string.tutorial_attendance_title),
                ctx.getString(R.string.tutorial_attendance_message), Gravity.BOTTOM);
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
