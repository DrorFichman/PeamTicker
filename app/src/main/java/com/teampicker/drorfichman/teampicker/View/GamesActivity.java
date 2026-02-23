package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.teampicker.drorfichman.teampicker.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.List;

public class GamesActivity extends AppCompatActivity {
    private static final String EXTRA_PLAYER_FILTERS = "EXTRA_PLAYER_FILTERS";
    private static final String EXTRA_EDITABLE = "EXTRA_EDITABLE";

    private ArrayList<String> mPlayers = new ArrayList<>();
    private boolean mEditable;

    @NonNull
    public static Intent getGameActivityIntent(Context context, List<String> players, boolean editable) {
        Intent intent = new Intent(context, GamesActivity.class);
        intent.putStringArrayListExtra(EXTRA_PLAYER_FILTERS, new ArrayList<>(players));
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
            gamesFragment = GamesFragment.newInstance(mPlayers, mEditable, this::onGamesCount);
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
        ArrayList<String> players = intent.getStringArrayListExtra(EXTRA_PLAYER_FILTERS);
        if (players != null) mPlayers = players;
        mEditable = intent.getBooleanExtra(EXTRA_EDITABLE, true);

        if (!mPlayers.isEmpty()) {
            setTitle(getTitle() + " : " + TextUtils.join(" + ", mPlayers));
        }
    }

    public void onGamesCount(int count) {
        setTitle(getString(R.string.games_with_count, count));
    }
}
