package com.teampicker.drorfichman.teampicker.tools.tutorials;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.Done;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.NotApplicable;
import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialStepStatus.ToDo;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.teampicker.drorfichman.teampicker.Data.Configurations;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.AuthHelper;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;

public class TutorialCloud extends AbstractTutorialStep {

    static TutorialCloud instance = new TutorialCloud();

    @Override
    public TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx) {
        // true if has games but didn't sign in and synced to cloud
        boolean hasGamesCloud = DbHelper.getGames(ctx, 3).size() > 2;
        boolean isSignedIn = AuthHelper.getUser() != null;
        boolean actionSync = TutorialManager.isActionTakenByUser(ctx, TutorialManager.TutorialUserAction.click_sync_to_cloud);
        boolean isCloudSupported = Configurations.isCloudFeatureSupported();
        if (!isCloudSupported || !hasGamesCloud) return NotApplicable;
        else if (!isSignedIn || !actionSync) return ToDo;
        else return Done;
    }

    @Override
    public void display(Activity activity, View targetView, boolean forceShow) {
        TutorialManager.showSpotlight(activity, targetView, prefKey(),
                forceShow, activity.getString(R.string.tutorial_cloud_title),
                activity.getString(R.string.tutorial_cloud_message));
    }

    @Override
    public String prefKey() {
        return PreferenceHelper.pref_skip_tutorial_cloud;
    }

    @Override
    public String name() {
        return "Sync to cloud";
    }
}
