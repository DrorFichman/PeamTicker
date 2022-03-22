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

public class TutorialTeamAnalysis extends AbstractTutorialStep {

    public static TutorialTeamAnalysis instance = new TutorialTeamAnalysis();

    @Override
    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if have more than 10 games but never clicked analysis
        boolean hasGamesForAnalysis = DbHelper.getGames(ctx, 11).size() > 10;
        boolean clickedAnalysis = TutorialManager.isActionTakenByUser(ctx, TutorialManager.TutorialUserAction.clicked_analysis);
        if (!hasGamesForAnalysis) return NotApplicable;
        else if (!clickedAnalysis) return ToDo;
        else return Done;
    }

    @Override
    public void display(Context ctx, boolean forceShow) {
        TutorialManager.showTutorialDialog(ctx, prefKey(),
                forceShow, ctx.getString(R.string.tutorial_team_analysis_title),
                ctx.getString(R.string.tutorial_team_analysis_message), Gravity.CENTER);
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
