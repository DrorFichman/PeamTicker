package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.Adapter.PlayerChemistryAdapter;
import com.teampicker.drorfichman.teampicker.Controller.Search.FilterView;
import com.teampicker.drorfichman.teampicker.Controller.Sort.SortType;
import com.teampicker.drorfichman.teampicker.Controller.Sort.Sorting;
import com.teampicker.drorfichman.teampicker.Data.BuilderPlayerCollaborationStatistics;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerChemistry;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.ColorHelper;
import com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;

import java.util.ArrayList;
import java.util.HashMap;

public class PlayerTeamFragment extends Fragment implements Sorting.sortingCallbacks {

    private static final String ARG_PLAYER = "player";
    private static final String ARG_BLUE_TEAM = "blue_team";
    private static final String ARG_ORANGE_TEAM = "orange_team";

    private final ArrayList<PlayerChemistry> players = new ArrayList<>();
    private PlayerChemistryAdapter playersAdapter;
    private Player pPlayer;

    private ArrayList<Player> blue;
    private ArrayList<Player> orange;

    private int games = 50;
    private final Sorting sorting = new Sorting(this, SortType.gamesWith);

    private ListView playersList;
    private View titles;
    private TextView name;
    private FilterView filterView;
    private View chip50Games;

    public PlayerTeamFragment() {
        super(R.layout.layout_chemistry_fragment);
    }

    public static PlayerTeamFragment newInstance(Player p,
                                                 ArrayList<Player> blueTeam, ArrayList<Player> orangeTeam) {
        PlayerTeamFragment fragment = new PlayerTeamFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLAYER, p);
        args.putSerializable(ARG_BLUE_TEAM, blueTeam);
        args.putSerializable(ARG_ORANGE_TEAM, orangeTeam);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        // Handle case where player data is missing (e.g., after process death with invalid data)
        if (pPlayer == null && getActivity() != null) {
            getActivity().finish();
            return root;
        }

        assert root != null;
        playersList = root.findViewById(R.id.players_participation_list);
        playersList.setOnItemClickListener(onPlayerClick);

        titles = root.findViewById(R.id.titles);
        name = root.findViewById(R.id.player_name);
        chip50Games = root.findViewById(R.id.participation_chip_50_games);

        setTeamIcon(root);

        setHeadlines(root);

        refreshPlayers();

        setGamesCountSelection(root);

        setHasOptionsMenu(true);

        return root;
    }

    private void setGamesCountSelection(View root) {
        ((RadioGroup) root.findViewById(R.id.participation_chip_group_games)).setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId == R.id.participation_chip_10_games) {
                        games = 10;
                        refreshPlayers();
                    } else if (checkedId == R.id.participation_chip_50_games) {
                        games = 50;
                        refreshPlayers();
                    } else {
                        games = -1;
                        refreshPlayers();
                    }
                });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore arguments (handles process death recreation)
        if (getArguments() != null) {
            pPlayer = (Player) getArguments().getSerializable(ARG_PLAYER);
            blue = (ArrayList<Player>) getArguments().getSerializable(ARG_BLUE_TEAM);
            orange = (ArrayList<Player>) getArguments().getSerializable(ARG_ORANGE_TEAM);
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPress);
        backPress.setEnabled(false);
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

    private boolean handleBackPress() {
        return ((filterView != null && filterView.isExpanded()));
    }

    private void setSearchView(SearchView view) {
        filterView = new FilterView(view, value -> {
            playersAdapter.setFilter(value);
            playersList.smoothScrollToPosition(playersAdapter.positionOfFirstFilterItem(() ->
                    Snackbar.make(requireContext(), playersList, "no results", Snackbar.LENGTH_SHORT).show()));
            backPress.setEnabled(handleBackPress());
        });
        if (playersAdapter != null) playersAdapter.setFilter(null);
    }

    private void setHeadlines(View root) {
        sorting.setHeadlineSorting(root, R.id.player_name, null, SortType.name);
        sorting.setHeadlineSorting(root, R.id.part_games_count_with, this.getString(R.string.games_with), SortType.gamesWith);
        sorting.setHeadlineSorting(root, R.id.part_wins_percentage_with, this.getString(R.string.success_with), SortType.successWith);
        sorting.setHeadlineSorting(root, R.id.part_games_count_against, this.getString(R.string.games_vs), SortType.gamesVs);
        sorting.setHeadlineSorting(root, R.id.part_wins_percentage_against, this.getString(R.string.success_vs), SortType.successVs);

        sorting.setSelected(root.findViewById(R.id.part_games_count_with));
    }

    private void setTeamIcon(View root) {

        int[] teamsIcons = ColorHelper.getTeamsIcons(getActivity());

        ImageView teamIcon = root.findViewById(R.id.team_icon);
        if (orange != null && orange.contains(pPlayer)) {
            teamIcon.setImageResource(teamsIcons[0]);
            teamIcon.setVisibility(View.VISIBLE);
        } else if (blue != null && blue.contains(pPlayer)) {
            teamIcon.setImageResource(teamsIcons[1]);
            teamIcon.setVisibility(View.VISIBLE);
        } else {
            teamIcon.setVisibility(View.INVISIBLE);
        }
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
            final Runnable r = () -> ScreenshotHelper.takeListScreenshot(getActivity(),
                    playersList, titles, playersAdapter);
            new Handler().postDelayed(r, 200);
        }

        return super.onOptionsItemSelected(item);
    }

    private final AdapterView.OnItemClickListener onPlayerClick = (parent, view, position, id) -> {
        Event.logEvent(FirebaseAnalytics.getInstance(requireContext()), EventType.player_collaboration_clicked);
        String selected = ((PlayerChemistry) parent.getItemAtPosition(position)).mName;
        Intent gameActivityIntent = GamesActivity.getGameActivityIntent(getContext(), pPlayer.mName, selected, false);
        startActivity(gameActivityIntent);
    };

    private void refreshPlayers() {
        Context context = getContext();
        if (context == null || pPlayer == null) return;

        HashMap<String, PlayerChemistry> result = DbHelper.getPlayersParticipationStatistics(context, pPlayer.mName,
                new BuilderPlayerCollaborationStatistics().setGames(games));
        players.clear();
        players.addAll(result.values());

        setTitle(context);

        sorting.sort(players);

        playersAdapter = new PlayerChemistryAdapter(context, players, blue, orange);
        playersList.setAdapter(playersAdapter);

        setGameCountValues();
    }

    private void setGameCountValues() {
        int totalGames = DbHelper.getGames(getContext(), pPlayer.mName).size();
        chip50Games.setVisibility(totalGames > 50 ? View.VISIBLE : View.GONE);
    }

    private void setTitle(Context context) {
        Player player = DbHelper.getPlayer(context, pPlayer.mName, games);
        name.setText(getString(R.string.player_participation_statistics,
                player.mName,
                player.statistics.gamesCount,
                player.statistics.getWinRate()));
    }

    //region sort
    @Override
    public void sortingChanged() {
        refreshPlayers();
    }
    //endregion
}
