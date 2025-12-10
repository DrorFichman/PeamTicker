package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.NotApplicable;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.ToDo;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialUserAction.clicked_teams;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;

public class TutorialSaveResults extends AbstractTutorialStep {

    static TutorialSaveResults instance = new TutorialSaveResults();

    @Override
    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if No games exist, but initial teams were made
        boolean hasInitialTeams = TutorialManager.isActionTakenByUser(ctx, clicked_teams);
        boolean hasGamesResults = DbHelper.getGames(ctx, 1).size() > 0;
        if (!hasInitialTeams) return NotApplicable;
        else if (!hasGamesResults) return ToDo;
        else return Done;
    }

    @Override
    public void display(Activity activity, View targetView, boolean forceShow) {
        TutorialManager.showSpotlight(activity, targetView, prefKey(),
                forceShow, activity.getString(R.string.tutorial_save_results_title),
                activity.getString(R.string.tutorial_save_results_message));
    }

    @Override
    public String prefKey() {
        return PreferenceHelper.pref_skip_tutorial_save;
    }

    @Override
    public String name() {
        return "Save results";
    }
}
