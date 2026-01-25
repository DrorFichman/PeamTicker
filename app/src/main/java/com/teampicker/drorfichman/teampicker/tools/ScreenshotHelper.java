package com.teampicker.drorfichman.teampicker.tools;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.core.content.FileProvider;

/**
 * Created by drorfichman on 11/11/16.
 */
public class ScreenshotHelper {

    public static void takeListScreenshot(Activity activity, ListView list, View titles, ArrayAdapter adapter) {
        try {
            Bitmap bitmap = getWholeListViewItemsToBitmap(list, titles, adapter);

            Uri imageUri = saveImageUri(activity, bitmap);

            openScreenshot(activity, imageUri);
        } catch (Throwable e) {
            Toast.makeText(activity, "Failed to take screenshot: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public static Uri saveImageUri(Context context, Bitmap bitmap) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".webp";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/webp");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                // WebP at 90% quality provides excellent quality with small file sizes
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, out);
            }
            return uri;
        } else {
            throw new IOException("Failed to save image");
        }
    }

    public static void takeScreenshot(Activity activity, View view) {
        try {
            // Capture the bitmap from the provided view
            Bitmap bitmap = getBitmapFromView(view);

            // Save the bitmap to a file
            Uri imageUri = saveImageUri(activity, bitmap);

            // Open the screenshot for preview
            openScreenshot(activity, imageUri);

        } catch (Throwable e) {
            // Handle potential errors
            Toast.makeText(activity, "Failed to take screenshot: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private static File getImageFromBitmap(Bitmap bitmap) throws IOException {
        String name = DateHelper.getNow() + "-" + System.currentTimeMillis();

        File imagePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/TeamPicker/");
        imagePath.mkdirs();
        File imageFile = new File(imagePath, name + ".webp");

        FileOutputStream outputStream = new FileOutputStream(imageFile);
        // WebP at 90% quality provides excellent quality with small file sizes
        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, outputStream);
        outputStream.flush();
        outputStream.close();

        return imageFile;
    }

    public static Bitmap getBitmapFromView(View view) {
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    private static Bitmap getWholeListViewItemsToBitmap(ListView listview, View titles, ArrayAdapter adapter) {

        listview.setDrawingCacheEnabled(true);

        int itemCount = adapter.getCount();
        int totalHeight = 0;
        List<Bitmap> images = new ArrayList<>();

        titles.setDrawingCacheEnabled(true);
        titles.setBackgroundColor(Color.parseColor("#ede6e6"));
        Bitmap bitmap = Bitmap.createBitmap(titles.getDrawingCache());
        titles.setBackgroundColor(Color.parseColor("#ffffff"));
        titles.setDrawingCacheEnabled(false);
        images.add(bitmap);
        totalHeight += titles.getMeasuredHeight();

        for (int i = 0; i < itemCount && i < 50; i++) {

            View childView = adapter.getView(i, null, listview);
            childView.measure(View.MeasureSpec.makeMeasureSpec(listview.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            measureView(childView);

            images.add(childView.getDrawingCache());
            totalHeight += childView.getMeasuredHeight();
        }

        Bitmap fullBitmap = Bitmap.createBitmap(listview.getMeasuredWidth(), totalHeight, Bitmap.Config.ARGB_8888);
        Canvas fullCanvas = new Canvas(fullBitmap);

        Paint paint = new Paint();
        int iHeight = 0;

        for (int i = 0; i < images.size(); i++) {
            Bitmap bmp = images.get(i);
            fullCanvas.drawBitmap(bmp, 0, iHeight, paint);
            iHeight += bmp.getHeight();

            bmp.recycle();
        }

        listview.setDrawingCacheEnabled(false);

        return fullBitmap;
    }

    private static void measureView(View childView) {
        childView.layout(0, 0, childView.getMeasuredWidth(), childView.getMeasuredHeight());
        childView.setDrawingCacheEnabled(true);
        childView.buildDrawingCache();
        childView.setBackgroundColor(Color.parseColor("#ede6e6"));
    }

    private static void openScreenshot(Context context, Uri uri) {
        openScreenshot(context, uri, null);
    }

    /**
     * Open screenshot with optional pre-filled text for sharing
     * @param context The context
     * @param uri The image URI
     * @param shareText Optional text to include with the image (null for no text)
     */
    public static void openScreenshot(Context context, Uri uri, String shareText) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setDataAndType(uri, "image/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        
        // Add text if provided
        if (shareText != null && !shareText.isEmpty()) {
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
        }
        
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share Teams"));
    }

    /**
     * Share directly to WhatsApp with pre-filled text
     * @param context The context
     * @param uri The image URI
     * @param shareText Text to include with the image
     */
    public static void shareToWhatsApp(Context context, Uri uri, String shareText) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.setPackage("com.whatsapp");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        
        if (shareText != null && !shareText.isEmpty()) {
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
        }
        
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            // WhatsApp not installed, fall back to general share
            Toast.makeText(context, "WhatsApp not installed. Opening share menu...", Toast.LENGTH_SHORT).show();
            openScreenshot(context, uri, shareText);
        }
    }

    /**
     * Generate default share text for team lineups
     * @param context The context for string resources
     * @param teamSize The number of players per team
     * @return Formatted share text
     */
    public static String generateTeamShareText(Context context, int teamSize) {
        return generateTeamShareText(context, teamSize, null);
    }

    /**
     * Generate share text for team lineups with optional game date
     * @param context The context for string resources
     * @param teamSize The number of players per team
     * @param gameDate Optional game date string (null for no date)
     * @return Formatted share text
     */
    public static String generateTeamShareText(Context context, int teamSize, String gameDate) {
        StringBuilder text = new StringBuilder();
        
        if (gameDate != null && !gameDate.isEmpty()) {
            text.append("⚽ Game on ").append(gameDate).append("\n\n");
        } else {
            text.append("⚽ Today's Teams\n\n");
        }
        
        text.append("Teams are ready! ");
        text.append(teamSize).append(" vs ").append(teamSize);
        text.append("\n\nSee you on the field! 🔥");
        
        return text.toString();
    }

    /**
     * Generate share text for game results
     * @param context The context
     * @param team1Score Team 1 score
     * @param team2Score Team 2 score
     * @param gameDate Optional game date
     * @return Formatted share text
     */
    public static String generateResultShareText(Context context, int team1Score, int team2Score, String gameDate) {
        StringBuilder text = new StringBuilder();
        
        text.append("⚽ Final Score");
        if (gameDate != null && !gameDate.isEmpty()) {
            text.append(" - ").append(gameDate);
        }
        text.append("\n\n");
        
        text.append("🔵 Team 1: ").append(team1Score).append("\n");
        text.append("🟠 Team 2: ").append(team2Score).append("\n\n");
        
        if (team1Score > team2Score) {
            text.append("🏆 Team 1 wins!");
        } else if (team2Score > team1Score) {
            text.append("🏆 Team 2 wins!");
        } else {
            text.append("🤝 It's a tie!");
        }
        
        text.append("\n\nGreat game everyone! 👏");
        
        return text.toString();
    }

    /**
     * Generate "invite friends" share text for app promotion
     * @param context The context
     * @return Formatted invite text
     */
    public static String generateAppInviteText(Context context) {
        return "⚽ Check out Team Picker!\n\n" +
               "I'm using this app to create fair teams for our games. " +
               "It has AI-powered balancing, WhatsApp integration, and tracks player statistics.\n\n" +
               "Free on Google Play:\n" +
               "https://play.google.com/store/apps/details?id=com.teampicker.drorfichman.teampicker";
    }
}
