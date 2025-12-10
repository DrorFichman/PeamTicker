package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.ToDo;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;

public class TutorialNewPlayer extends AbstractTutorialStep {

    static TutorialNewPlayer instance = new TutorialNewPlayer();

    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if no player currently exist
        boolean hasPlayers = DbHelper.getPlayers(ctx, 0, false).size() > 0;
        return hasPlayers ? Done : ToDo;
    }

    @Override
    public void display(Activity activity, View targetView, boolean forceShow) {
        TutorialManager.showSpotlight(activity, targetView, prefKey(),
                forceShow, activity.getString(R.string.tutorial_create_player_title),
                activity.getString(R.string.tutorial_create_player_message));
    }

    @Override
    public String prefKey() {
        return PreferenceHelper.pref_skip_tutorial_players;
    }

    public String name() {
        return "Create players";
    }
}
