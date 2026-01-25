package com.teampicker.drorfichman.teampicker.tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;

import java.io.IOException;

/**
 * Enhanced social sharing features for team lineups and game results
 */
public class SocialShareHelper {

    /**
     * Show share options dialog for team lineup
     * @param activity The activity context
     * @param view The view to capture as screenshot
     * @param teamSize Number of players per team
     * @param gameDate Optional game date (null if not available)
     */
    public static void shareTeamLineup(Activity activity, View view, int teamSize, String gameDate) {
        // Show dialog with share options
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Share Teams");
        
        String[] options = {
            "Share to WhatsApp",
            "Share to other apps",
            "Share without text"
        };
        
        builder.setItems(options, (dialog, which) -> {
            try {
                // Capture screenshot
                Bitmap bitmap = com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.getBitmapFromView(view);
                Uri imageUri = com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.saveImageUri(activity, bitmap);
                
                switch (which) {
                    case 0: // WhatsApp with text
                        String whatsappText = com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.generateTeamShareText(activity, teamSize, gameDate);
                        com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.shareToWhatsApp(activity, imageUri, whatsappText);
                        Event.logEvent(FirebaseAnalytics.getInstance(activity), EventType.send_teams);
                        break;
                        
                    case 1: // Other apps with text
                        String shareText = com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.generateTeamShareText(activity, teamSize, gameDate);
                        com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.openScreenshot(activity, imageUri, shareText);
                        Event.logEvent(FirebaseAnalytics.getInstance(activity), EventType.send_teams);
                        break;
                        
                    case 2: // No text
                        com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.openScreenshot(activity, imageUri, null);
                        Event.logEvent(FirebaseAnalytics.getInstance(activity), EventType.send_teams);
                        break;
                }
            } catch (Exception e) {
                Toast.makeText(activity, "Failed to share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Share game results with scores
     * @param activity The activity context
     * @param view The view to capture
     * @param team1Score Team 1 score
     * @param team2Score Team 2 score
     * @param gameDate Optional game date
     */
    public static void shareGameResult(Activity activity, View view, int team1Score, int team2Score, String gameDate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Share Results");
        
        String[] options = {
            "Share to WhatsApp",
            "Share to other apps"
        };
        
        builder.setItems(options, (dialog, which) -> {
            try {
                Bitmap bitmap = com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.getBitmapFromView(view);
                Uri imageUri = com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.saveImageUri(activity, bitmap);
                String resultText = com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.generateResultShareText(activity, team1Score, team2Score, gameDate);
                
                if (which == 0) {
                    com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.shareToWhatsApp(activity, imageUri, resultText);
                } else {
                    com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.openScreenshot(activity, imageUri, resultText);
                }
                
                Event.logEvent(FirebaseAnalytics.getInstance(activity), EventType.send_teams);
            } catch (Exception e) {
                Toast.makeText(activity, "Failed to share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Share app invite to friends
     * @param context The context
     */
    public static void shareAppInvite(Context context) {
        String inviteText = com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper.generateAppInviteText(context);
        
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, inviteText);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Check out Team Picker!");
        
        context.startActivity(Intent.createChooser(intent, "Invite Friends"));
        
        Event.logEvent(FirebaseAnalytics.getInstance(context), EventType.send_teams);
    }

    /**
     * Share milestone achievement
     * @param context The context
     * @param milestoneText The milestone description (e.g., "10 games tracked!")
     */
    public static void shareMilestone(Context context, String milestoneText) {
        String shareText = "⚽ " + milestoneText + "\n\n" +
                "I'm using Team Picker to organize fair soccer teams.\n" +
                "Free on Google Play:\n" +
                "https://play.google.com/store/apps/details?id=com.teampicker.drorfichman.teampicker";
        
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        context.startActivity(Intent.createChooser(intent, "Share Achievement"));
    }

}

