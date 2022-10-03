package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.NotApplicable;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.ToDo;

import android.content.Context;
import android.view.Gravity;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;

public class TutorialPickTeams extends AbstractTutorialStep {

    static TutorialPickTeams instance = new TutorialPickTeams();

    @Override
    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if never clicked shuffle button
        boolean actionShuffle = TutorialManager.isActionTakenByUser(ctx, TutorialManager.TutorialUserAction.clicked_shuffle);
        boolean hasComingPlayersForPickActions = DbHelper.getComingPlayers(ctx, 0).size() > 0;
        if (!hasComingPlayersForPickActions) return NotApplicable;
        else if (!actionShuffle) return ToDo;
        else return Done;
    }

    @Override
    public void display(Context ctx, boolean forceShow) {
        TutorialManager.showTutorialDialog(ctx, prefKey(),
                forceShow, ctx.getString(R.string.tutorial_pick_teams_title),
                ctx.getString(R.string.tutorial_pick_teams_message), Gravity.CENTER);
    }

    @Override
    public String prefKey() {
        return PreferenceHelper.pref_skip_tutorial_pick;
    }

    @Override
    public String name() {
        return "Teams options";
    }
}
