package com.teampicker.drorfichman.teampicker.View;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.teampicker.drorfichman.teampicker.Adapter.GameExpandableAdapter;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Controller.Sort.Sorting;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GamesFragment extends Fragment {

    private int gamesCount = 50;
    private List<String> mPlayers = new ArrayList<>();
    private gamesCountHandler mCountHandler;
    private boolean mEditable;

    private ExpandableListView gamesList;
    private View gamesContent;
    private View emptyGamesStateContainer;

    private GameResultBroadcast notificationHandler;
    private final Sorting sorting = new Sorting(null, null);
    private Button newGameButton;

    public interface gamesCountHandler {
        void onGamesCount(int count);
    }

    public GamesFragment() {
        super(R.layout.layout_games_fragment);
    }

    public static GamesFragment newInstance(List<String> players, boolean editable, gamesCountHandler handler) {
        GamesFragment gamesFragment = new GamesFragment();
        gamesFragment.mPlayers = players != null ? new ArrayList<>(players) : new ArrayList<>();
        gamesFragment.mCountHandler = handler;
        gamesFragment.mEditable = editable;
        return gamesFragment;
    }

    private boolean isAllGamesView() {
        return mPlayers.isEmpty();
    }

    private boolean isSinglePlayerView() {
        return mPlayers.size() == 1;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(isAllGamesView()); // options menu (game count filter) only shown in all-games view

        gamesList = root.findViewById(R.id.games_list);
        gamesContent = root.findViewById(R.id.games_content);
        emptyGamesStateContainer = root.findViewById(R.id.empty_games_state_container);
        newGameButton = root.findViewById(R.id.games_list_make_teams);

        setHeadlines(root);
        setNewGameButton();
        refreshGames();

        return root;
    }

    private void setNewGameButton() {
        updateNewGameButtonVisibility();

        newGameButton.setOnClickListener(view -> {
            Intent makeTeamsIntent = MakeTeamsActivity.getIntent(getContext());
            if (makeTeamsIntent != null) {
                MakeTeamsActivity.setResult(makeTeamsIntent);
                startActivity(makeTeamsIntent);
            } else {
                Toast.makeText(getContext(), getString(R.string.toast_instruction_select_players_first), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNewGameButtonVisibility() {
        // Only show "new game" button when viewing all games AND there's an active game with team divisions
        boolean hasActiveGame = DbHelper.hasActiveGame(getContext());
        newGameButton.setVisibility(isAllGamesView() && hasActiveGame ? View.VISIBLE : View.GONE); // new game only in all-games view
    }

    private void setHeadlines(View root) {
        sorting.setHeadlineSorting(root, R.id.game_date, getString(R.string.date), null);
        sorting.setHeadlineSorting(root, R.id.game_result_set_divider, getString(R.string.score), null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.games_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_last_50_games:
                gamesCount = 50;
                refreshGames();
                break;
            case R.id.action_all_games:
                gamesCount = -1;
                refreshGames();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationHandler = new GameResultBroadcast();
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.GAME_UPDATE_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PULL_DATA_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.SETTING_MODIFIED_ACTION, notificationHandler); // team colors
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNewGameButtonVisibility();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalNotifications.unregisterBroadcastReceiver(getContext(), notificationHandler);
    }

    public void refreshGames() {
        Context activity = getContext();
        if (activity == null) return;

        ArrayList<Game> games;
        if (isAllGamesView()) {
            games = DbHelper.getGames(activity, gamesCount);
        } else {
            games = DbHelper.getGames(activity, mPlayers);
            // When filtering by 2+ players, show only games where they were on the same team
            if (mPlayers.size() >= 2) {
                games.removeIf(g -> !g.playersAllOnSameTeam);
            }
        }

        if (mCountHandler != null) {
            mCountHandler.onGamesCount(games.size());
        }

        GameExpandableAdapter gamesAdapter = new GameExpandableAdapter(activity, games,
                mPlayers, mEditable, this::refreshGames, gamesList);
        gamesList.setAdapter(gamesAdapter);

        boolean isEmpty = games.isEmpty();
        gamesContent.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyGamesStateContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    //region broadcasts
    class GameResultBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshGames();
            updateNewGameButtonVisibility();
        }
    }
    //endregion
}