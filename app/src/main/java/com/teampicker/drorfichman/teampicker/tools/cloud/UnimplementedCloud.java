package com.teampicker.drorfichman.teampicker.tools.cloud;

import android.content.Context;
import android.widget.Toast;

import com.teampicker.drorfichman.teampicker.R;

public class UnimplementedCloud implements CloudSync {
    @Override
    public void syncToCloud(Context ctx, SyncProgress handler) {
    }

    @Override
    public void pullFromCloud(Context ctx, SyncProgress handler) {
    }

    @Override
    public void storeAccountData() {
    }
}
