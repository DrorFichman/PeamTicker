package com.teampicker.drorfichman.teampicker.tools;

import android.Manifest;
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
        PermissionTools.checkPermissionsForExecution(activity, 1, () -> {
            try {
                Bitmap bitmap = getWholeListViewItemsToBitmap(list, titles, adapter);

                Uri imageUri = saveImageUri(activity, bitmap, 100);

                openScreenshot(activity, imageUri);
            } catch (Throwable e) {
                Toast.makeText(activity, "Failed to take screenshot: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }, Manifest.permission.READ_MEDIA_IMAGES);
    }

    private static Uri saveImageUri(Context context, Bitmap bitmap, int quality) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            }
            return uri;
        } else {
            throw new IOException("Failed to save image");
        }
    }

    public static void takeScreenshot(Activity activity, View view) {
        PermissionTools.checkPermissionsForExecution(activity, 1, () -> {
            try {
                // Capture the bitmap from the provided view
                Bitmap bitmap = getBitmapFromView(view);

                // Save the bitmap to a file
                Uri imageUri = saveImageUri(activity, bitmap, 50);

                // Open the screenshot for preview
                openScreenshot(activity, imageUri);

            } catch (Throwable e) {
                // Handle potential errors
                Toast.makeText(activity, "Failed to take screenshot: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }, Manifest.permission.READ_MEDIA_IMAGES); // Only READ_EXTERNAL_STORAGE is needed
    }

    private static File getImageFromBitmap(Bitmap bitmap, int quality) throws IOException {
        String name = DateHelper.getNow() + "-" + System.currentTimeMillis();
        if (quality < 0) quality = 50;

        File imagePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/TeamPicker/");
        imagePath.mkdirs();
        File imageFile = new File(imagePath, name + ".png");

        FileOutputStream outputStream = new FileOutputStream(imageFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream);
        outputStream.flush();
        outputStream.close();

        return imageFile;
    }

    private static Bitmap getBitmapFromView(View view) {
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
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setDataAndType(uri, "image/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share Teams"));
    }
}
