package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.NotApplicable;
import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.ToDo;

import android.content.Context;
import android.view.Gravity;

import com.google.android.material.snackbar.Snackbar;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;
import com.teampicker.drorfichman.teampicker.tools.TutorialManager;

public class TutorialStartPickTeams extends AbstractTutorialStep {

    public static TutorialStartPickTeams instance = new TutorialStartPickTeams();

    @Override
    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if never clicked teams button, but has checked players
        boolean actionStartPick = TutorialManager.isActionTakenByUser(ctx, TutorialManager.TutorialUserAction.clicked_teams);
        boolean hasComingPlayersForPick = DbHelper.getComingPlayers(ctx, 0).size() > 0;
        if (!hasComingPlayersForPick) return NotApplicable;
        else if (!actionStartPick) return ToDo;
        else return Done;
    }

    @Override
    public void display(Context ctx, boolean forceShow) {
        TutorialManager.showTutorialDialog(ctx, prefKey(),
                forceShow, ctx.getString(R.string.tutorial_start_pick_teams_title),
                ctx.getString(R.string.tutorial_start_pick_teams_message), Gravity.CENTER);
    }

    @Override
    public String prefKey() {
        return PreferenceHelper.pref_skip_tutorial_start_teams;
    }

    @Override
    public String name() {
        return "Pick teams";
    }
}
