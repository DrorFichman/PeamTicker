package com.teampicker.drorfichman.teampicker.Data;

import android.content.Context;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.Exclude;
import com.teampicker.drorfichman.teampicker.BuildConfig;
import com.teampicker.drorfichman.teampicker.tools.AuthHelper;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;
import com.teampicker.drorfichman.teampicker.tools.cloud.queries.GetConfiguration;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by drorfichman on 7/27/16.
 */
public class Configurations implements Serializable {

    public static Configurations remote = null;

    public boolean allowCloudFeatures = false;
    public ArrayList<String> allowedAccounts = new ArrayList<>();
    public int oldestSupportedVersion = 0;
    public String oldestSupportedVersionMessage = "";

    public Configurations() {
    }

    @Exclude
    public static boolean isCloudFeaturesAllowed() {
        if (remote == null) return false;
        FirebaseUser user = AuthHelper.getUser();
        return (remote.allowCloudFeatures ||
                (user != null && remote.allowedAccounts.contains(user.getEmail())));
    }

    @Exclude
    public static boolean isCloudFeatureSupported() {
        if (remote == null) return true;
        return (remote.allowCloudFeatures || remote.allowedAccounts.size() > 0);
    }

    @Exclude
    public static boolean isVersionSupported() {
        if (remote == null) return true;
        else return BuildConfig.VERSION_CODE >= remote.oldestSupportedVersion;
    }

    @Exclude
    public static String outdatedVersionMessage() {
        if (remote == null) return "";
        else return remote.oldestSupportedVersionMessage;
    }

    @Exclude
    @Override
    public String toString() {
        return "Configurations{" +
                "allowCloudFeatures=" + allowCloudFeatures +
                ", allowedAccounts=" + allowedAccounts +
                ", oldestSupportedVersion=" + oldestSupportedVersion +
                '}';
    }
}
