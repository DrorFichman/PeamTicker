package com.teampicker.drorfichman.teampicker.tools.cloud.queries;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.teampicker.drorfichman.teampicker.Data.AccountData;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class GetUsers {

    public interface Results {
        void queryResults(ArrayList<AccountData> result);
    }

    public static void query(Context ctx, Results caller) {
        ValueEventListener users = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<AccountData> users = new ArrayList<>();
                for (DataSnapshot snapshotNode : snapshot.getChildren()) {
                    AccountData a = snapshotNode.child(FirebaseHelper.Node.account.name()).getValue(AccountData.class);
                    if (a != null && !TextUtils.isEmpty(a.displayName) && !TextUtils.isEmpty(a.uid))
                        users.add(a);
                    Log.i("getUsers", "Account " + a);
                }

                caller.queryResults(users);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("getUsers", "onCancelled", error.toException());
            }
        };
        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(users);
    }
}
