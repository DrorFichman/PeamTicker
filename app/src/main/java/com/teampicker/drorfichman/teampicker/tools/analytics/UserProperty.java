package com.teampicker.drorfichman.teampicker.tools.analytics;

import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

public class UserProperty {

    public static void log(FirebaseAnalytics analytics, UserPropertyType type, String value) {
        Log.i("Analytics", "User property " + type.name() + " = " + value);
        analytics.setUserProperty(type.name(), value);
    }
}
