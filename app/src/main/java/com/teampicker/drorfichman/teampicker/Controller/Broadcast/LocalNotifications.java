package com.teampicker.drorfichman.teampicker.Controller.Broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class LocalNotifications {

    public static String GAME_UPDATE_ACTION = "com.teampicker.drorfichman.teampicker.game_update";
    public static String PLAYER_UPDATE_ACTION = "com.teampicker.drorfichman.teampicker.player_update";
    public static String PULL_DATA_ACTION = "com.teampicker.drorfichman.teampicker.pull_data";
    public static String SETTING_MODIFIED_ACTION = "com.teampicker.drorfichman.teampicker.setting_modified";

    public static void sendNotification(Context ctx, String action) {
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
    }

    public static void registerBroadcastReceiver(Context ctx, String action, BroadcastReceiver br) {
        IntentFilter filter = new IntentFilter(action);
        LocalBroadcastManager.getInstance(ctx).registerReceiver(br, filter);
    }

    public static void unregisterBroadcastReceiver(Context ctx, BroadcastReceiver br) {
        LocalBroadcastManager.getInstance(ctx).unregisterReceiver(br);
    }
}
