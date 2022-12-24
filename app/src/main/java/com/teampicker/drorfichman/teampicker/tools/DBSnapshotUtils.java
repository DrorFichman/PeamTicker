package com.teampicker.drorfichman.teampicker.tools;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.ajts.androidmads.library.ExcelToSQLite;
import com.ajts.androidmads.library.SQLiteToExcel;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.PlayerContract;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;

import java.io.File;

import androidx.core.app.ActivityCompat;

/**
 * Created by drorfichman on 4/14/18.
 */
public class DBSnapshotUtils {

    private static final String SNAPSHOT_FILE_NAME = "peamticker_snapshot";
    private static final String EXPORT_PATH = "/TeamPicker/DBs/";

    public interface ExportListener {
        void exportStarted();

        void exportCompleted(File filePath);

        void exportError(String msg);
    }

    public interface ImportListener {
        void preImport();

        void importStarted();

        void importCompleted();

        void importError(String msg);
    }

    public static void takeDBSnapshot(Activity activity, ExportListener listener, String name) {

        PermissionTools.checkPermissionsForExecution(activity, 3,
                () -> takeDBSnapshotPermitted(activity, listener, name),
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);

        Event.logEvent(FirebaseAnalytics.getInstance(activity), EventType.backup_to_file);
    }

    public static File getSnapshotPath() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + EXPORT_PATH);
    }

    private static String getSnapshotFileName() {
        return SNAPSHOT_FILE_NAME + "-" + DateHelper.getNow() + ".xls";
    }

    private static void takeDBSnapshotPermitted(final Context ctx, final ExportListener listener, String name) {

        final File dbs = getSnapshotPath();
        dbs.mkdirs();

        String snapshot = TextUtils.isEmpty(name) ? getSnapshotFileName() : name;

        Log.d("snapshot", "takeDBSnapshotPermitted " + dbs.getAbsolutePath() + " - " + snapshot);

        SQLiteToExcel sqliteToExcel = new SQLiteToExcel(ctx, DbHelper.DATABASE_NAME, dbs.getAbsolutePath());
        sqliteToExcel.exportSpecificTables(PlayerContract.getTables(), snapshot,
                new SQLiteToExcel.ExportListener() {
                    @Override
                    public void onStart() {
                        if (listener != null) listener.exportStarted();
                    }

                    @Override
                    public void onCompleted(String filePath) {
                        if (listener != null) listener.exportCompleted(new File(dbs, snapshot));
                    }

                    @Override
                    public void onError(Exception e) {
                        if (listener != null) listener.exportError(e.getMessage());
                        Log.e("EXPORT", "Failed export", e);
                    }
                });
    }

    public static void importDBSnapshotSelected(final Context ctx, String snapshotPath,
                                                final ImportListener listener) {

        Log.d("snapshot", "importDBSnapshotSelected Import from " + snapshotPath);

        if (TextUtils.isEmpty(snapshotPath)) {
            if (listener != null)
                listener.importError("attempt importing file from local file manager");
            return;
        }

        // Remove all existing DB content
        DbHelper.deleteTableContents(ctx);

        if (listener != null) listener.preImport();

        ExcelToSQLite excelToSQLite = new ExcelToSQLite(ctx.getApplicationContext(), DbHelper.DATABASE_NAME, false);
        excelToSQLite.importFromFile(snapshotPath, new ExcelToSQLite.ImportListener() {
            @Override
            public void onStart() {
                if (listener != null) listener.importStarted();
            }

            @Override
            public void onCompleted(String dbName) {
                if (listener != null) listener.importCompleted();
            }

            @Override
            public void onError(Exception e) {
                Log.e("IMPORT", "Failed import", e);
                if (listener != null) listener.importError(e.getMessage());
            }
        });
    }
}
