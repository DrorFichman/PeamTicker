package com.teampicker.drorfichman.teampicker.Data;

import android.content.Context;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.Exclude;
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

    public Configurations() {
    }

    @Exclude
    public static boolean isCloudFeaturesAllowed() {
        if (remote == null) return false;
        FirebaseUser user = AuthHelper.getUser();
        return (remote.allowCloudFeatures ||
                (user != null && remote.allowedAccounts.contains(user.getEmail())));
    }

    public static boolean isCloudFeatureSupported() {
        if (remote == null) return true;
        return (remote.allowCloudFeatures || remote.allowedAccounts.size() > 0);
    }
}
