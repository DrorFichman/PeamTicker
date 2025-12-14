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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class PlayerChemistryActivity extends AppCompatActivity {
    private static final String EXTRA_PLAYER = "EXTRA_PLAYER";
    private static final String EXTRA_BLUE_PLAYERS = "EXTRA_BLUE_PLAYERS";
    private static final String EXTRA_ORANGE_PLAYERS = "EXTRA_ORANGE_PLAYERS";
    private static final String EXTRA_SHOW_CHART = "EXTRA_SHOW_CHART";
    private static final String EXTRA_RECENT_GAMES = "EXTRA_RECENT_GAMES";

    private Player pPlayer;
    private ArrayList<Player> blue;
    private ArrayList<Player> orange;
    private boolean showChart;
    private int recentGames;

    @NonNull
    public static Intent getPlayerParticipationActivity(Context context, String playerName,
                                                        ArrayList<Player> blue, ArrayList<Player> orange) {
        Intent intent = new Intent(context, PlayerChemistryActivity.class);
        intent.putExtra(PlayerChemistryActivity.EXTRA_PLAYER, playerName);
        intent.putExtra(PlayerChemistryActivity.EXTRA_BLUE_PLAYERS, blue);
        intent.putExtra(PlayerChemistryActivity.EXTRA_ORANGE_PLAYERS, orange);
        intent.putExtra(PlayerChemistryActivity.EXTRA_SHOW_CHART, false);
        return intent;
    }

    /**
     * Creates an intent to show the player collaboration chart with team colors.
     * Players in playerTeam will be colored with their team color,
     * players in opposingTeam will be colored with the opposing team's color.
     *
     * @param context The context
     * @param playerName The player whose collaboration chart to show
     * @param playerTeam The team the player belongs to
     * @param opposingTeam The opposing team
     * @param recentGames Number of recent games to use for statistics
     * @return Intent to start this activity with the collaboration chart
     */
    @NonNull
    public static Intent getPlayerCollaborationChartActivity(Context context, String playerName,
                                                             ArrayList<Player> playerTeam, ArrayList<Player> opposingTeam,
                                                             int recentGames) {
        Intent intent = new Intent(context, PlayerChemistryActivity.class);
        intent.putExtra(PlayerChemistryActivity.EXTRA_PLAYER, playerName);
        // For the chart: player's team goes first (team1), opposing team goes second (team2)
        intent.putExtra(PlayerChemistryActivity.EXTRA_BLUE_PLAYERS, playerTeam);
        intent.putExtra(PlayerChemistryActivity.EXTRA_ORANGE_PLAYERS, opposingTeam);
        intent.putExtra(PlayerChemistryActivity.EXTRA_SHOW_CHART, true);
        intent.putExtra(PlayerChemistryActivity.EXTRA_RECENT_GAMES, recentGames);
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
            showChart = intent.getBooleanExtra(EXTRA_SHOW_CHART, false);
            recentGames = intent.getIntExtra(EXTRA_RECENT_GAMES, 50);
        }

        Fragment existingFragment = getSupportFragmentManager().findFragmentById(R.id.collaboration_container);

        if (existingFragment == null) {
            Fragment fragment;
            if (showChart) {
                // Show team collaboration chart
                // blue contains the player's team, orange contains the opposing team
                fragment = PlayerTeamCollaborationChartFragment.newInstance(
                        DbHelper.getPlayer(this, pPlayer.mName), blue, orange, recentGames);
            } else {
                // Show chemistry list fragment
                fragment = PlayerTeamFragment.newInstance(
                        DbHelper.getPlayer(this, pPlayer.mName), blue, orange);
            }
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.collaboration_container, fragment);
            transaction.commit();
        }
    }
}
