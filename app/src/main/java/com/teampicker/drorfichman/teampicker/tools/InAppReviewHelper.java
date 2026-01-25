package com.teampicker.drorfichman.teampicker.tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;

/**
 * Helper class to manage in-app review prompts using Google Play Core API
 */
public class InAppReviewHelper {
    private static final String TAG = "InAppReviewHelper";
    
    // Minimum number of games saved before showing review
    private static final int MIN_GAMES_FOR_REVIEW = 3;
    
    // Minimum time between review prompts (90 days in milliseconds)
    private static final long MIN_TIME_BETWEEN_REVIEWS = 90L * 24 * 60 * 60 * 1000;

    /**
     * Check if we should request a review and launch the in-app review flow if appropriate
     * 
     * @param activity The activity context (must be an Activity, not just Context)
     */
    public static void requestReviewIfAppropriate(Activity activity) {
        if (!shouldRequestReview(activity)) {
            Log.d(TAG, "Review request skipped - conditions not met");
            return;
        }

        Event.logEvent(FirebaseAnalytics.getInstance(activity), EventType.in_app_review_requested);
        
        ReviewManager reviewManager = ReviewManagerFactory.create(activity);
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = reviewManager.launchReviewFlow(activity, reviewInfo);
                
                flow.addOnCompleteListener(flowTask -> {
                    // The flow has finished, regardless of whether user reviewed or not
                    // Record that we've shown the prompt
                    recordReviewShown(activity);
                    Event.logEvent(FirebaseAnalytics.getInstance(activity), EventType.in_app_review_shown);
                    
                    if (flowTask.isSuccessful()) {
                        Event.logEvent(FirebaseAnalytics.getInstance(activity), EventType.in_app_review_completed);
                        Log.d(TAG, "In-app review flow completed");
                    }
                });
            } else {
                Log.e(TAG, "Review request failed", task.getException());
            }
        });
    }

    /**
     * Determine if we should show the review prompt based on:
     * 1. Number of games saved (3+)
     * 2. Time since last review prompt (90+ days)
     * 3. Not shown too many times
     * 
     * @param context The application context
     * @return true if we should request review
     */
    private static boolean shouldRequestReview(Context context) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        
        // Check 1: Has user saved at least 3 games?
        int totalGames = DbHelper.getTotalGamesCount(context);
        if (totalGames < MIN_GAMES_FOR_REVIEW) {
            Log.d(TAG, "Not enough games saved: " + totalGames + " < " + MIN_GAMES_FOR_REVIEW);
            return false;
        }
        
        // Check 2: Has enough time passed since last review?
        long lastReviewTime = prefs.getLong(PreferenceHelper.pref_last_review_request_time, 0);
        long currentTime = System.currentTimeMillis();
        long timeSinceLastReview = currentTime - lastReviewTime;
        
        if (lastReviewTime > 0 && timeSinceLastReview < MIN_TIME_BETWEEN_REVIEWS) {
            Log.d(TAG, "Too soon since last review: " + (timeSinceLastReview / (24 * 60 * 60 * 1000)) + " days");
            return false;
        }
        
        // Check 3: Haven't shown review too many times (max 3 times total)
        int reviewCount = prefs.getInt(PreferenceHelper.pref_review_request_count, 0);
        if (reviewCount >= 3) {
            Log.d(TAG, "Maximum review requests reached: " + reviewCount);
            return false;
        }
        
        Log.d(TAG, "Review conditions met - will show review");
        return true;
    }

    /**
     * Record that we've shown the review prompt
     */
    private static void recordReviewShown(Context context) {
        SharedPreferences prefs = PreferenceHelper.getSharedPreference(context);
        
        // Update last review time
        PreferenceHelper.setSharedPreferenceLong(context, 
                PreferenceHelper.pref_last_review_request_time, 
                System.currentTimeMillis());
        
        // Increment review count
        int currentCount = prefs.getInt(PreferenceHelper.pref_review_request_count, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PreferenceHelper.pref_review_request_count, currentCount + 1);
        editor.apply();
        
        Log.d(TAG, "Recorded review shown. Count: " + (currentCount + 1));
    }
}

