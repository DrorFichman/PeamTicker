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
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
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

    private int games = -1;
    Sorting sorting = new Sorting(this::sortingChanged, SortType.success);
    private StatisticsResultBroadcast notificationHandler;

    private ArrayList<Player> players;

    private PlayerStatisticsAdapter playersAdapter;
    private ListView playersList;
    private View titles;
    private TextView statsTotals;
    private View gameCountSelection;
    private View chip50Games;
    private FilterView filterView;
    private String currentFilterValue;

    public StatisticsFragment() {
        super(R.layout.layout_statistics_fragment);
    }

    public static StatisticsFragment newInstance() {
        return new StatisticsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        assert root != null;
        setPlayersList(root);
        setGamesCountSelection(root);

        return root;
    }

    private void setGamesCountSelection(View root) {
        ((ChipGroup) root.findViewById(R.id.stats_chip_group_games)).setOnCheckedChangeListener(
                (group, checkedChip) -> {
                    if (checkedChip == R.id.stat_chip_10_games) {
                        games = 10;
                        refreshPlayers(true);
                    } else if (checkedChip == R.id.stat_chip_50_games) {
                        games = 50;
                        refreshPlayers(true);
                    } else {
                        games = -1;
                        refreshPlayers(true);
                    }
                });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationHandler = new StatisticsFragment.StatisticsResultBroadcast();
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PLAYER_UPDATE_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.GAME_UPDATE_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PULL_DATA_ACTION, notificationHandler);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPress);
        backPress.setEnabled(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalNotifications.unregisterBroadcastReceiver(getContext(), notificationHandler);
    }

    private void setPlayersList(View root) {
        gameCountSelection = root.findViewById(R.id.stats_chip_games);
        chip50Games = root.findViewById(R.id.stat_chip_50_games);
        statsTotals = root.findViewById(R.id.stats_total_values);
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

        playersAdapter = new PlayerStatisticsAdapter(getContext(), players, getGamesCountFilter(), showInternalData);
        playersList.setAdapter(playersAdapter);

        // Re-apply filter if active
        if (currentFilterValue != null && !currentFilterValue.isEmpty()) {
            playersAdapter.setFilter(currentFilterValue);
        }

        setGameCountValues();
    }

    private void setGameCountValues() {
        int gamesCount = getGamesCountFilter();
        int totalGames = DbHelper.getGames(getContext()).size();
        gameCountSelection.setVisibility(totalGames > 10 ? View.VISIBLE : View.GONE);
        chip50Games.setVisibility(totalGames >= 50 ? View.VISIBLE : View.GONE);
        statsTotals.setText(getString(R.string.stats_with_count, players.size(), gamesCount));
    }

    private boolean handleBackPress() {
        return ((filterView != null && filterView.isExpanded()));
    }

    final OnBackPressedCallback backPress = new OnBackPressedCallback(true) {

        @Override
        public void handleOnBackPressed() {
            if (filterView != null && filterView.isExpanded()) {
                filterView.collapseSearchView();
                backPress.setEnabled(handleBackPress());
            }
        }
    };

    private void setSearchView(SearchView view) {
        filterView = new FilterView(view, value -> {
            currentFilterValue = value;
            playersAdapter.setFilter(value);
            playersList.smoothScrollToPosition(playersAdapter.positionOfFirstFilterItem(() ->
                    Snackbar.make(requireContext(), playersList, "no results", Snackbar.LENGTH_SHORT).show()));
            backPress.setEnabled(handleBackPress());
        });
        if (playersAdapter != null) playersAdapter.setFilter(null);
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
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.statisctics_menu, menu);
        setSearchView((SearchView) menu.findItem(R.id.action_stat_search_players).getActionView());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_send_statistics) {
            takeScreenshot();
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