package com.teampicker.drorfichman.teampicker.tools;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.core.content.FileProvider;

/**
 * Created by drorfichman on 11/11/16.
 */
public class ScreenshotHelper {

    public static void takeListScreenshot(Activity activity, ListView list, View titles, ArrayAdapter adapter) {

        PermissionTools.checkPermissionsForExecution(activity, 1, () -> {
            try {
                Bitmap bitmap = getWholeListViewItemsToBitmap(list, titles, adapter);

                File imageFile = getImageFromBitmap(bitmap, 100);

                openScreenshot(activity, imageFile);

            } catch (Throwable e) {

                // Several error may come out with file handling or OOM
                Toast.makeText(activity, "Failed to take screenshot " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    public static void takeScreenshot(Activity activity, View view) {

        PermissionTools.checkPermissionsForExecution(activity, 1, () -> {
            try {
                Bitmap bitmap = getBitmapFromView(view);

                File imageFile = getImageFromBitmap(bitmap, 50);

                openScreenshot(activity, imageFile);

            } catch (Throwable e) {

                // Several error may come out with file handling or OOM
                Toast.makeText(activity, "Failed to take screenshot " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
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

    private static void openScreenshot(Context ctx, File imageFile) {

        Intent intent = new Intent(Intent.ACTION_SEND);
        Uri photoURI = FileProvider.getUriForFile(ctx,
                ctx.getApplicationContext().getPackageName() + ".team.picker.share.screenshot",
                imageFile);

        intent.putExtra(Intent.EXTRA_STREAM, photoURI);
        intent.setType("image/*");
        ctx.startActivity(Intent.createChooser(intent, "Share screenshot"));
    }
}
