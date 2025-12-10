package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.NotApplicable;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.ToDo;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;

public class TutorialShuffleAI extends AbstractTutorialStep {

    static TutorialShuffleAI instance = new TutorialShuffleAI();

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
    public void display(Activity activity, View targetView, boolean forceShow) {
        TutorialManager.showSpotlight(activity, targetView, prefKey(),
                forceShow, activity.getString(R.string.tutorial_team_shuffle_stats_title),
                activity.getString(R.string.tutorial_team_shuffle_stats_message));
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
