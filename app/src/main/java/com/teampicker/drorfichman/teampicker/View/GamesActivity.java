package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.teampicker.drorfichman.teampicker.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class GamesActivity extends AppCompatActivity {
    private static final String EXTRA_PLAYER_FILTER = "EXTRA_PLAYER_FILTER";
    private static final String EXTRA_PLAYER_COLLABORATOR = "EXTRA_PLAYER_COLLABORATOR";
    private static final String EXTRA_EDITABLE = "EXTRA_EDITABLE";

    private String mPlayerName;
    private String mPlayerCollaborator;
    private boolean mEditable;

    @NonNull
    public static Intent getGameActivityIntent(Context context, String playerName, String collaborator, boolean editable) {
        Intent intent = new Intent(context, GamesActivity.class);
        intent.putExtra(EXTRA_PLAYER_FILTER, playerName);
        intent.putExtra(EXTRA_PLAYER_COLLABORATOR, collaborator);
        intent.putExtra(EXTRA_EDITABLE, editable);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_games_activity);

        getPlayerIntent();

        GamesFragment gamesFragment = getFragment();
        if (gamesFragment == null) {
            gamesFragment = GamesFragment.newInstance(mPlayerName, mPlayerCollaborator, mEditable, this::onGamesCount);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.games_container, gamesFragment);
            transaction.commit();
        }
    }

    private GamesFragment getFragment() {
        return (GamesFragment) getSupportFragmentManager().findFragmentById(R.id.games_container);
    }

    private void getPlayerIntent() {
        Intent intent = getIntent();
        mPlayerName = intent.getStringExtra(EXTRA_PLAYER_FILTER);
        mPlayerCollaborator = intent.getStringExtra(EXTRA_PLAYER_COLLABORATOR);
        mEditable = intent.getBooleanExtra(EXTRA_EDITABLE, true);

        String addTitle = "";
        if (mPlayerName != null) addTitle = " : " + mPlayerName;
        if (mPlayerName != null && mPlayerCollaborator != null)
            addTitle += " + " + mPlayerCollaborator;
        setTitle(getTitle() + addTitle);
    }

    public void onGamesCount(int count) {
        setTitle(getString(R.string.games_with_count, count));
    }
}
