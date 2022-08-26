package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class PlayerChemistryActivity extends AppCompatActivity {
    private static final String EXTRA_PLAYER = "EXTRA_PLAYER";
    private static final String EXTRA_BLUE_PLAYERS = "EXTRA_BLUE_PLAYERS";
    private static final String EXTRA_ORANGE_PLAYERS = "EXTRA_ORANGE_PLAYERS";

    private Player pPlayer;
    private ArrayList<Player> blue;
    private ArrayList<Player> orange;

    @NonNull
    public static Intent getPlayerParticipationActivity(Context context, String playerName,
                                                        ArrayList<Player> blue, ArrayList<Player> orange) {
        Intent intent = new Intent(context, PlayerChemistryActivity.class);
        intent.putExtra(PlayerChemistryActivity.EXTRA_PLAYER, playerName);
        intent.putExtra(PlayerChemistryActivity.EXTRA_BLUE_PLAYERS, blue);
        intent.putExtra(PlayerChemistryActivity.EXTRA_ORANGE_PLAYERS, orange);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_chemistry_activity);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_PLAYER)) {
            pPlayer = DbHelper.getPlayer(this, intent.getStringExtra(EXTRA_PLAYER));
            orange = (ArrayList<Player>) intent.getSerializableExtra(EXTRA_ORANGE_PLAYERS);
            blue = (ArrayList<Player>) intent.getSerializableExtra(EXTRA_BLUE_PLAYERS);
        }

        PlayerChemistryFragment participationFragment =
                (PlayerChemistryFragment) getSupportFragmentManager().findFragmentById(R.id.collaboration_container);

        if (participationFragment == null) {
            participationFragment = PlayerChemistryFragment.newInstance(DbHelper.getPlayer(this, pPlayer.mName), blue, orange);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.collaboration_container, participationFragment);
            transaction.commit();
        }
    }
}
