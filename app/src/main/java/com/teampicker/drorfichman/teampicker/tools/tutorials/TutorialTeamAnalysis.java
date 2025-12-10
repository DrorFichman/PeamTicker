package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.NotApplicable;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.ToDo;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;

public class TutorialTeamAnalysis extends AbstractTutorialStep {

    static TutorialTeamAnalysis instance = new TutorialTeamAnalysis();

    @Override
    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if have more than 10 games but never clicked analysis
        boolean hasGamesForAnalysis = DbHelper.getGames(ctx, 11).size() > 1;
        boolean clickedAnalysis = TutorialManager.isActionTakenByUser(ctx, TutorialManager.TutorialUserAction.clicked_analysis);
        Log.d("TUT", "hasGamesForAnalysis " + hasGamesForAnalysis + ", clickedAnalysis " + clickedAnalysis);
        if (!hasGamesForAnalysis) return NotApplicable;
        else if (!clickedAnalysis) return ToDo;
        else return Done;
    }

    @Override
    public void display(Activity activity, View targetView, boolean forceShow) {
        TutorialManager.showSpotlight(activity, targetView, prefKey(),
                forceShow, activity.getString(R.string.tutorial_team_analysis_title),
                activity.getString(R.string.tutorial_team_analysis_message));
    }

    @Override
    public String prefKey() {
        return PreferenceHelper.pref_skip_tutorial_analysis;
    }

    @Override
    public String name() {
        return "Team Analysis";
    }
}
