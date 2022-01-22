package com.teampicker.drorfichman.teampicker.View;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.teampicker.drorfichman.teampicker.Adapter.PlayerStatisticsAdapter;
import com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper;
import com.teampicker.drorfichman.teampicker.Controller.Sort.Sorting;
import com.teampicker.drorfichman.teampicker.Controller.Sort.SortType;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class StatisticsActivity extends AppCompatActivity implements Sorting.sortingCallbacks {
    private int games = 50;

    private ListView playersList;
    TextView gradeTitle;

    private PlayerStatisticsAdapter playersAdapter;
    Sorting sorting = new Sorting(this::sortingChanged, SortType.success);

    private static final int ACTIVITY_RESULT_PLAYER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_statistics_activity);

        setPlayersList();
    }

    private void setPlayersList() {
        playersList = findViewById(R.id.players_statistics_list);

        refreshPlayers();

        setHeadlines();

        playersList.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent intent = PlayerDetailsActivity.getDetailsPlayerIntent(StatisticsActivity.this, (String) view.getTag(R.id.player_id));
            startActivityForResult(intent, ACTIVITY_RESULT_PLAYER);
        });

        playersList.setOnItemLongClickListener((adapterView, view, i, l) -> false);
    }

    private void setHeadlines() {
        gradeTitle = findViewById(R.id.stat_player_grade);
        sorting.setHeadlineSorting(this, R.id.stat_player_grade, this.getString(R.string.grade), SortType.grade);
        sorting.setHeadlineSorting(this, R.id.player_name, this.getString(R.string.name), SortType.name);
        sorting.setHeadlineSorting(this, R.id.stat_success, this.getString(R.string.success), SortType.success);
        sorting.setHeadlineSorting(this, R.id.stat_games_count, this.getString(R.string.games), SortType.games);
        sorting.setHeadlineSorting(this, R.id.stat_wins_percentage, this.getString(R.string.win_rate), SortType.winPercentage);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.statisctics_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send_statistics:

                enterSendMode();

                final Runnable r = () -> {
                    ScreenshotHelper.takeListScreenshot(StatisticsActivity.this,
                            playersList, findViewById(R.id.titles), playersAdapter);
                    exitSendMode();
                };

                new Handler().postDelayed(r, 400);

                break;

            case R.id.action_last_10_games:
                games = 10;
                refreshPlayers();
                break;
            case R.id.action_last_50_games:
                games = 50;
                refreshPlayers();
                break;
            case R.id.action_no_limit:
                games = -1;
                refreshPlayers();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exitSendMode();
                Toast.makeText(this, "We're ready! you can now share your screenshot :)", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void enterSendMode() {
        gradeTitle.setVisibility(View.INVISIBLE);
        refreshPlayers(false);
    }

    private void exitSendMode() {
        gradeTitle.setVisibility(View.VISIBLE);
        refreshPlayers(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTIVITY_RESULT_PLAYER && resultCode > 0) {
            Log.d("TEAMS", "localDataChanged statistics list on save");
            refreshPlayers();
        }
    }

    public void refreshPlayers() {
        refreshPlayers(true);
    }

    public void refreshPlayers(boolean showInternalData) {
        ArrayList<Player> players = DbHelper.getPlayersStatistics(this, games);

        sorting.sort(players);

        playersAdapter = new PlayerStatisticsAdapter(this, players,  getGamesCountFilter(), showInternalData);
        playersList.setAdapter(playersAdapter);

        setTitle(getString(R.string.stats_with_count, players.size(), getGamesCountFilter()));
    }

    private int getGamesCountFilter() {
        if (games > 0) return games;
        else return DbHelper.getGames(this).size();
    }

    @Override
    public void sortingChanged() {
        refreshPlayers();
    }
}
