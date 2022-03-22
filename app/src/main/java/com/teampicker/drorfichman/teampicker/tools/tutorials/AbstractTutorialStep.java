package com.teampicker.drorfichman.teampicker.tools.tutorials;

import android.content.Context;

import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;
import com.teampicker.drorfichman.teampicker.tools.TutorialManager;

public abstract class AbstractTutorialStep {

    public abstract String name();

    public abstract TutorialManager.TutorialStepStatus shouldBeDisplayed(Context ctx);

    public abstract void display(Context ctx, boolean forceShow);

    public boolean isDialogDismissedByUser(Context ctx) {
        return PreferenceHelper.getSharedPreference(ctx).contains(prefKey());
    }

    public abstract String prefKey();
}
