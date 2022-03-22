package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.NotApplicable;
import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.ToDo;
import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialUserAction.clicked_teams;

import android.content.Context;
import android.view.Gravity;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;
import com.teampicker.drorfichman.teampicker.tools.TutorialManager;

public class TutorialSaveResults extends AbstractTutorialStep {

    public static TutorialSaveResults instance = new TutorialSaveResults();

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
    public void display(Context ctx, boolean forceShow) {
        TutorialManager.showTutorialDialog(ctx, prefKey(),
                forceShow, ctx.getString(R.string.tutorial_save_results_title),
                ctx.getString(R.string.tutorial_save_results_message), Gravity.CENTER);
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
