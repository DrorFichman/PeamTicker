package com.teampicker.drorfichman.teampicker.tools.cloud.queries;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.teampicker.drorfichman.teampicker.Data.Configurations;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;

public class GetConfiguration {

    public interface Results {
        void queryResults(Configurations result);
    }

    public static void query(Context ctx, Results caller) {
        ValueEventListener query = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Configurations conf = dataSnapshot.getValue(Configurations.class);
                Log.i("GetConfiguration", conf.toString());

                caller.queryResults(conf);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("GetConfiguration", "onCancelled", databaseError.toException());
            }
        };
        FirebaseHelper.configurations().addListenerForSingleValueEvent(query);
    }
}
