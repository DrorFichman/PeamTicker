package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.NotApplicable;
import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.ToDo;

import android.content.Context;
import android.view.Gravity;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;
import com.teampicker.drorfichman.teampicker.tools.TutorialManager;

public class TutorialShuffleAI extends AbstractTutorialStep {

    public static TutorialShuffleAI instance = new TutorialShuffleAI();

    @Override
    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if have more than 10 games but never clicked analysis
        boolean hasGamesForStats = DbHelper.getGames(ctx, 11).size() > 10;
        boolean clickedShuffleStats = TutorialManager.isActionTakenByUser(ctx, TutorialManager.TutorialUserAction.clicked_shuffle_stats);
        if (!hasGamesForStats) return NotApplicable;
        else if (!clickedShuffleStats) return ToDo;
        else return Done;
    }

    @Override
    public void display(Context ctx, boolean forceShow) {
        TutorialManager.showTutorialDialog(ctx, prefKey(),
                forceShow, ctx.getString(R.string.tutorial_team_shuffle_stats_title),
                ctx.getString(R.string.tutorial_team_shuffle_stats_message), Gravity.CENTER);
    }

    @Override
    public String prefKey() {
        return PreferenceHelper.pref_skip_tutorial_shuffle_stats;
    }

    @Override
    public String name() {
        return "Shuffle A.I.";
    }
}
