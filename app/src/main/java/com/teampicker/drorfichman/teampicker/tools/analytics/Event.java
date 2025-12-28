package com.teampicker.drorfichman.teampicker.tools.analytics;

import android.os.Bundle;
import android.util.Log;

import androidx.core.util.Pair;

import com.google.firebase.analytics.FirebaseAnalytics;

public class Event {

    EventType eventType;
    Bundle parameters;

    @SafeVarargs
    public Event(EventType event, Pair<ParameterType, String>... values) {
        eventType = event;
        parameters = new Bundle();
        for (Pair<ParameterType, String> a : values) {
            set(a.first, a.second);
        }
    }

    public void set(ParameterType key, String value) {
        parameters.putString(key.name(), value);
    }

    public static void logEvent(FirebaseAnalytics analytics, EventType type) {
        new Event(type).log(analytics);
    }

    public void log(FirebaseAnalytics analytics) {
        Log.i("Analytics", "Analytics " + this.eventType.name());
        analytics.logEvent(this.eventType.name(), this.parameters);
    }
}
