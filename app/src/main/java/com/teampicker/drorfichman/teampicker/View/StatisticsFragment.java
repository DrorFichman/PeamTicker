package com.teampicker.drorfichman.teampicker.View;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.teampicker.drorfichman.teampicker.Adapter.PlayerStatisticsAdapter;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Controller.Search.FilterView;
import com.teampicker.drorfichman.teampicker.Controller.Sort.SortType;
import com.teampicker.drorfichman.teampicker.Controller.Sort.Sorting;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper;

import java.util.ArrayList;

public class StatisticsFragment extends Fragment {

    private int games = 50;
    Sorting sorting = new Sorting(this::sortingChanged, SortType.success);
    private StatisticsResultBroadcast notificationHandler;

    private ArrayList<Player> players;

    private PlayerStatisticsAdapter playersAdapter;
    private ListView playersList;
    private View titles;
    private FilterView filterView;

    public StatisticsFragment() {
        super(R.layout.layout_statistics_activity);
    }

    public static StatisticsFragment newInstance() {
        StatisticsFragment fragment = new StatisticsFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        setSearchView(root);
        setPlayersList(root);

        return root;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationHandler = new StatisticsFragment.StatisticsResultBroadcast();
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PLAYER_UPDATE_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.GAME_UPDATE_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PULL_DATA_ACTION, notificationHandler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalNotifications.unregisterBroadcastReceiver(getContext(), notificationHandler);
    }

    private void setPlayersList(View root) {
        playersList = root.findViewById(R.id.players_statistics_list);

        refreshPlayers(true);

        setHeadlines(root);

        playersList.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent intent = PlayerDetailsActivity.getEditPlayerIntent(getContext(), (String) view.getTag(R.id.player_id));
            startActivity(intent);
        });

        playersList.setOnItemLongClickListener((adapterView, view, i, l) -> false);
    }

    public void refreshPlayers(boolean showInternalData) {
        players = DbHelper.getPlayersStatistics(getContext(), games);

        sorting.sort(players);

        playersAdapter = new PlayerStatisticsAdapter(getContext(), players,  getGamesCountFilter(), showInternalData);
        playersAdapter.setFilter(filterView.getFilter());
        playersList.setAdapter(playersAdapter);
    }

    private void setSearchView(View root) {
        filterView = new FilterView(root.findViewById(R.id.stat_search_players), value -> {
            playersAdapter.setFilter(value);
        });
    }

    private int getGamesCountFilter() {
        int gameCount = DbHelper.getGames(getContext()).size();
        if (games > 0) return Math.min(games, gameCount);
        else return gameCount;
    }

    private void setHeadlines(View root) {
        titles = root.findViewById(R.id.titles);
        sorting.setHeadlineSorting(titles, R.id.stat_player_grade, this.getString(R.string.grade), SortType.grade);
        sorting.setHeadlineSorting(titles, R.id.player_name, this.getString(R.string.name), SortType.name);
        sorting.setHeadlineSorting(titles, R.id.stat_success, this.getString(R.string.success), SortType.success);
        sorting.setHeadlineSorting(titles, R.id.stat_games_count, this.getString(R.string.games), SortType.games);
        sorting.setHeadlineSorting(titles, R.id.stat_wins_percentage, this.getString(R.string.win_rate), SortType.winPercentage);

        sorting.setSelected(root.findViewById(R.id.stat_success));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.statisctics_menu, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        boolean notifyGameCount = false;

        switch (item.getItemId()) {
            case R.id.action_stat_search_players:
                filterView.toggleSearchVisibility();
                break;
            case R.id.action_send_statistics:
                takeScreenshot();
                break;
            case R.id.action_last_10_games:
                games = 10;
                notifyGameCount = true;
                refreshPlayers(true);
                break;
            case R.id.action_last_50_games:
                games = 50;
                notifyGameCount = true;
                refreshPlayers(true);
                break;
            case R.id.action_no_limit:
                games = -1;
                notifyGameCount = true;
                refreshPlayers(true);
                break;
        }

        if (notifyGameCount) {
            Toast.makeText(getContext(),
                    getString(R.string.stats_with_count, players.size(), getGamesCountFilter()),
                    Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void takeScreenshot() {
        titles.findViewById(R.id.stat_player_grade).setVisibility(View.INVISIBLE);
        refreshPlayers(false);

        final Runnable r = () -> {
            ScreenshotHelper.takeListScreenshot(getActivity(), playersList, titles, playersAdapter);
            titles.findViewById(R.id.stat_player_grade).setVisibility(View.VISIBLE);
            refreshPlayers(true);
        };

        new Handler().postDelayed(r, 400);
    }

    public void sortingChanged() {
        refreshPlayers(true);
    }

    //region broadcasts
    class StatisticsResultBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Broadcast stats", "new date");
            refreshPlayers(true);
        }
    }
    //endregion
}