package com.teampicker.drorfichman.teampicker.tools.cloud;

import com.google.firebase.auth.FirebaseUser;

public class AccountData { 

    public String email;
    public String displayName;
    public String uid;
    public String photoUrl;
    public long lastSignIn;
    public long creation;
    public long lastUsage;

    public AccountData() {
    }

    public AccountData(FirebaseUser user) {
        if (user != null) {
            this.email = user.getEmail();
            this.displayName = user.getDisplayName();
            this.uid = user.getUid();
            this.photoUrl = user.getPhotoUrl().toString();
            if (user.getMetadata() != null) {
                this.lastSignIn = user.getMetadata().getLastSignInTimestamp();
                this.creation = user.getMetadata().getCreationTimestamp();
            }
            this.lastUsage = System.currentTimeMillis();
        }
    }
}
