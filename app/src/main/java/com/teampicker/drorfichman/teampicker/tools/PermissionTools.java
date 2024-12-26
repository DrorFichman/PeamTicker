package com.teampicker.drorfichman.teampicker.tools;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class PermissionTools {

    public interface onPermissionGranted {
        void execute();
    }

    public static void checkPermissionsForExecution(Activity ctx, int requestCode, onPermissionGranted handler, String... check) {
        // Check if all permissions are granted
        boolean allGranted = true;
        for (String per : check) {
            if (ActivityCompat.checkSelfPermission(ctx, per) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            // If all permissions are granted, execute the handler
            handler.execute();
        } else {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(ctx, check, requestCode);
        }
    }
}
