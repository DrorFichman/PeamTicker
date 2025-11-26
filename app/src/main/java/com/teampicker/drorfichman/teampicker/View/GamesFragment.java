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

public class GamesFragment extends Fragment {

    private int gamesCount = 50;
    private String mPlayerName;
    private String mPlayerCollaborator;
    private gamesCountHandler mCountHandler;
    private boolean mEditable;

    private ExpandableListView gamesList;

    private GameResultBroadcast notificationHandler;
    private final Sorting sorting = new Sorting(null, null);
    private Button newGameButton;

    public interface gamesCountHandler {
        void onGamesCount(int count);
    }

    public GamesFragment() {
        super(R.layout.layout_games_fragment);
    }

    public static GamesFragment newInstance(String playerName, String collaborator, boolean editable, gamesCountHandler handler) {
        GamesFragment gamesFragment = new GamesFragment();
        gamesFragment.mPlayerName = playerName;
        gamesFragment.mPlayerCollaborator = collaborator;
        gamesFragment.mCountHandler = handler;
        gamesFragment.mEditable = editable;
        return gamesFragment;
    }

    private boolean isAllGamesView() {
        return (mPlayerName == null && mPlayerCollaborator == null);
    }

    private boolean isPlayerView() {
        return (mPlayerName != null && mPlayerCollaborator == null);
    }

    private boolean isPlayerCollaboratorView() {
        return (mPlayerName != null && mPlayerCollaborator != null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(isAllGamesView());

        gamesList = root.findViewById(R.id.games_list);
        newGameButton = root.findViewById(R.id.games_list_make_teams);

        setHeadlines(root);
        setNewGameButton();
        refreshGames();

        return root;
    }

    private void setNewGameButton() {
        newGameButton.setVisibility(isAllGamesView() ? View.VISIBLE : View.GONE);

        newGameButton.setOnClickListener(view -> {
            Intent makeTeamsIntent = MakeTeamsActivity.getIntent(getContext());
            if (makeTeamsIntent != null) {
                MakeTeamsActivity.setResult(makeTeamsIntent);
                startActivity(makeTeamsIntent);
            } else {
                Toast.makeText(getContext(), "First - select attending players", Toast.LENGTH_SHORT).show();
            }
        });
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
    public void onDestroy() {
        super.onDestroy();
        LocalNotifications.unregisterBroadcastReceiver(getContext(), notificationHandler);
    }

    public void refreshGames() {
        Context activity = getContext();
        if (activity == null) return;

        ArrayList<Game> games;
        if (isPlayerCollaboratorView()) { // games in which both played
            games = DbHelper.getGames(activity, mPlayerName, mPlayerCollaborator);
        } else if (isPlayerView()) { // games in which selected player played
            games = DbHelper.getGames(activity, mPlayerName);
        } else { // all games
            games = DbHelper.getGames(activity, gamesCount);
        }

        if (mCountHandler != null) {
            mCountHandler.onGamesCount(games.size());
        }

        GameExpandableAdapter gamesAdapter = new GameExpandableAdapter(activity, games,
                mPlayerName, mPlayerCollaborator, mEditable, this::refreshGames, gamesList);
        gamesList.setAdapter(gamesAdapter);
    }

    //region broadcasts
    class GameResultBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Broadcast games", "new data");
            refreshGames();
        }
    }
    //endregion
}