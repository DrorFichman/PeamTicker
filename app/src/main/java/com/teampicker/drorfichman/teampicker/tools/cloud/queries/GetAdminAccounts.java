package com.teampicker.drorfichman.teampicker.tools.cloud.queries;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.teampicker.drorfichman.teampicker.Data.Configurations;
import com.teampicker.drorfichman.teampicker.tools.cloud.AccountData;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;

import java.util.ArrayList;

import androidx.annotation.NonNull;

/**
 * Query to fetch admin accounts from Firebase based on the email list in Configurations.adminAccounts
 */
public class GetAdminAccounts {

    public interface Results {
        void queryResults(ArrayList<AccountData> result);
    }

    public static void query(Context ctx, Results caller) {
        if (Configurations.remote == null || Configurations.remote.adminAccounts.isEmpty()) {
            // No admin accounts configured, return empty list
            caller.queryResults(new ArrayList<>());
            return;
        }

        ValueEventListener users = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<AccountData> adminUsers = new ArrayList<>();
                
                // Create a set of admin emails for quick lookup
                ArrayList<String> adminEmails = Configurations.remote.adminAccounts;
                
                for (DataSnapshot snapshotNode : snapshot.getChildren()) {
                    AccountData a = snapshotNode.child(FirebaseHelper.Node.account.name()).getValue(AccountData.class);
                    if (a != null && !TextUtils.isEmpty(a.displayName) && !TextUtils.isEmpty(a.uid)) {
                        // Check if this account is in the admin list
                        if (adminEmails.contains(a.email)) {
                            adminUsers.add(a);
                            Log.i("getAdminAccounts", "Admin account found: " + a.displayName);
                        }
                    }
                }

                Log.d("getAdminAccounts", "Found " + adminUsers.size() + " admin accounts out of " + adminEmails.size() + " configured");
                caller.queryResults(adminUsers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("getAdminAccounts", "onCancelled", error.toException());
                caller.queryResults(new ArrayList<>());
            }
        };
        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(users);
    }
}

