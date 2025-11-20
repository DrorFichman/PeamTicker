package com.teampicker.drorfichman.teampicker.Data;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.Exclude;
import com.teampicker.drorfichman.teampicker.BuildConfig;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by drorfichman on 7/27/16.
 */
public class Configurations implements Serializable {

    public static Configurations remote = null;

    public boolean allowCloudFeatures = false;
    public ArrayList<String> allowedAccounts = new ArrayList<>();
    public ArrayList<String> adminAccounts = new ArrayList<>();
    public int oldestSupportedVersion = 0;
    public String oldestSupportedVersionMessage = "";
    public boolean weatherFeatureEnabled = true;

    public Configurations() {
    }

    public enum UserCloudState {
        Unknown,
        Disabled,
        RequireAuthentication,
        NotAllowed,
        Allowed;
    }

    @Exclude
    public static UserCloudState getUserCloudState(FirebaseUser user) {
        if (remote == null)
            return UserCloudState.Unknown;
        if (!isCloudFeatureSupported())
            return UserCloudState.Disabled;
        if (user == null)
            return UserCloudState.RequireAuthentication;
        if (isCloudFeatureAllowed(user)) {
            return UserCloudState.Allowed;
        } else {
            return UserCloudState.NotAllowed;
        }
    }

    @Exclude
    public static boolean isCloudFeatureSupported() {
        if (remote == null) return true; // assume it is

        // Unauthenticated user might (allowedAccounts) or definitely will have access
        return (remote.allowCloudFeatures ||
                remote.allowedAccounts.size() > 0);
    }

    @Exclude
    public static boolean isCloudFeatureAllowed(FirebaseUser user) {
        if (remote == null) return false; // assume it isn't

        // Authenticated user allowed generally (allowCloudFeatures) or explicitly (allowedAccounts)
        return (remote.allowCloudFeatures ||
                remote.allowedAccounts.contains(user.getEmail()));
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
    public static boolean isAdmin(FirebaseUser user) {
        if (remote == null) return false;
        else return remote.adminAccounts.contains(user.getEmail());
    }

    @Exclude
    public static boolean isWeatherFeatureEnabled() {
        if (remote == null) return true; // default to enabled if config not loaded
        return remote.weatherFeatureEnabled;
    }

    @Exclude
    @Override
    public String toString() {
        return "Configurations{" +
                "allowCloudFeatures=" + allowCloudFeatures +
                ", allowedAccounts=" + allowedAccounts +
                ", oldestSupportedVersion=" + oldestSupportedVersion +
                ", weatherFeatureEnabled=" + weatherFeatureEnabled +
                '}';
    }
}
