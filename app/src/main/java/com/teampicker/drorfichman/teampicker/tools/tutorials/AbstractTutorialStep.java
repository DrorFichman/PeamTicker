package com.teampicker.drorfichman.teampicker.tools.tutorials;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;

public abstract class AbstractTutorialStep {

    public abstract String name();

    public abstract TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx);

    /**
     * Display the tutorial spotlight on the target view
     * @param activity The activity context
     * @param targetView The view to highlight (can be null for tutorials shown from menu)
     * @param forceShow If true, show even if previously dismissed
     */
    public abstract void display(Activity activity, View targetView, boolean forceShow);

    public boolean isDialogDismissedByUser(Context ctx) {
        return PreferenceHelper.getSharedPreference(ctx).contains(prefKey());
    }

    public abstract String prefKey();
}
