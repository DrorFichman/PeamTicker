package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.ToDo;

import android.content.Context;
import android.view.Gravity;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;

public class TutorialNewPlayer extends AbstractTutorialStep {

    public static TutorialNewPlayer instance = new TutorialNewPlayer();

    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if no player currently exist
        boolean hasPlayers = DbHelper.getPlayers(ctx, 0, false).size() > 0;
        if (!hasPlayers) return ToDo;
        else return Done;
    }

    public void display(Context ctx, boolean forceShow) {
        TutorialManager.showTutorialDialog(ctx, prefKey(),
                forceShow, ctx.getString(R.string.tutorial_create_player_title),
                ctx.getString(R.string.tutorial_create_player_message), Gravity.BOTTOM);
    }

    @Override
    public String prefKey() {
        return PreferenceHelper.pref_skip_tutorial_players;
    }

    public String name() {
        return "Create players";
    }
}
