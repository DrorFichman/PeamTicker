package com.teampicker.drorfichman.teampicker.View;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.DateHelper;
import com.teampicker.drorfichman.teampicker.tools.DialogHelper;

import java.util.Calendar;

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

        if (!mEditable) {
            findViewById(R.id.edit_actions).setVisibility(View.GONE);
        }

        GamesFragment gamesFragment = getFragment();
        if (gamesFragment == null) {
            gamesFragment = GamesFragment.newInstance(mPlayerName, mPlayerCollaborator, this::onGamesCount);
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

    public void editGame(View view) {
        GamesFragment gameFragment = getFragment();
        if (gameFragment == null) return;

        Game game = gameFragment.getSelectedGame();
        if (game == null) {
            Toast.makeText(this, "Select game to edit", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar date = Calendar.getInstance();
        date.setTime(game.getDate());

        DatePickerDialog d = new DatePickerDialog(this, (datePicker, year, month, day) -> {
            Calendar selectedDate = new Calendar.Builder().setDate(year, month, day).build();
            if (selectedDate.getTimeInMillis() > Calendar.getInstance().getTimeInMillis())
                Toast.makeText(this, "Future date is not allowed", Toast.LENGTH_LONG).show();
            else
                updateGameDate(gameFragment, game, DateHelper.getDate(selectedDate.getTimeInMillis()));
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE));
        d.show();
    }

    private void updateGameDate(GamesFragment gameFragment, Game game, String date) {
        DbHelper.updateGameDate(this, game, date);
        gameFragment.refreshGames();
        Toast.makeText(this, "Game edited", Toast.LENGTH_SHORT).show();
    }

    public void deleteGame(View view) {
        GamesFragment gameFragment = getFragment();
        if (gameFragment == null) return;

        Game game = gameFragment.getSelectedGame();
        if (game == null) {
            Toast.makeText(this, "Select game to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogHelper.showApprovalDialog(this,
                getString(R.string.delete), "Do you want to remove (" + game.getDisplayDate(this) + ")?",
                ((dialog, which) -> {
                    DbHelper.deleteGame(this, game.gameId);
                    gameFragment.onGameSelected(null);
                    gameFragment.refreshGames();
                    Toast.makeText(this, "Game deleted", Toast.LENGTH_SHORT).show();
                })
        );
    }

    public void onGamesCount(int count) {
        setTitle(getString(R.string.games_with_count, count));
    }
}
