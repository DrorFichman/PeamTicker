package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.NotApplicable;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.ToDo;

import android.content.Context;
import android.view.Gravity;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;

public class TutorialGameHistory extends AbstractTutorialStep {

    public static TutorialGameHistory instance = new TutorialGameHistory();

    @Override
    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if has games, but never clicked a game in game history
        boolean hasMoreThanOneGamesHistory = DbHelper.getGames(ctx, 2).size() > 1;
        boolean clickedGame = TutorialManager.isActionTakenByUser(ctx, TutorialManager.TutorialUserAction.clicked_game_in_history);
        if (!hasMoreThanOneGamesHistory) return NotApplicable;
        else if (!clickedGame) return ToDo;
        else return Done;
    }

    @Override
    public void display(Context ctx, boolean forceShow) {
        TutorialManager.showTutorialDialog(ctx, prefKey(),
                forceShow, ctx.getString(R.string.tutorial_game_history_title),
                ctx.getString(R.string.tutorial_game_history_message), Gravity.CENTER);
    }

    @Override
    public String prefKey() {
        return PreferenceHelper.pref_skip_tutorial_history;
    }

    @Override
    public String name() {
        return "Game history";
    }
}
