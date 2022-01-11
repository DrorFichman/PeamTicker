package com.teampicker.drorfichman.teampicker.tools;

import android.app.Activity;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.teampicker.drorfichman.teampicker.Data.AccountData;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

public class AuthHelper {

    public static FirebaseUser requireLogin(Activity ctx, int activityResultCode) {
        FirebaseUser user = AuthHelper.getUser();
        if (user == null) {
            Log.i("AccountFB", "User not found");
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.GoogleBuilder().build());

            // Create and launch sign-in intent with Auth providers
            ctx.startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    activityResultCode);

            return null;
        } else {
            Log.i("AccountFB", "User found " + user.getEmail() + " - " + user.getUid());
            return user;
        }
    }

    public static FirebaseUser getUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public static void signOut() {
        if (FirebaseAuth.getInstance() != null) {
            FirebaseAuth.getInstance().signOut();
        }
    }

    @NonNull
    public static String getUserUID() {
        if (fetchUser != null && fetchUser.uid != null) {
            return fetchUser.uid;
        } else {
            return getUser() != null ? getUser().getUid() : "";
        }
    }

    // Allow fetching data on behalf of another user
    static AccountData fetchUser;
    public static void fetchFor(AccountData accountData) {
        fetchUser = accountData;
    }
}
