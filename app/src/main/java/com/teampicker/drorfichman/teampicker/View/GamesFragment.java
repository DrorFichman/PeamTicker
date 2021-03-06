package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.teampicker.drorfichman.teampicker.Adapter.GameAdapter;
import com.teampicker.drorfichman.teampicker.Adapter.PlayerTeamGameHistoryAdapter;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.TeamEnum;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.ColorHelper;
import com.teampicker.drorfichman.teampicker.tools.DialogHelper;

import java.util.ArrayList;
import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class GamesFragment extends Fragment {

    private String mPlayerName;
    private String mPlayerCollaborator;

    private ListView gamesList;
    private GameAdapter gamesAdapter;

    private int mCurrGameId;
    private Game mCurrGame;
    private ArrayList<Player> mTeam1;
    private ArrayList<Player> mTeam2;

    private View gameDetails;
    private ListView team1List;
    private ListView team2List;

    public GamesFragment() {
        super(R.layout.layout_games_activity_fragment);
    }

    public static GamesFragment newInstance(String playerName, String collaborator) {
        GamesFragment gamesFragment = new GamesFragment();
        gamesFragment.mPlayerName = playerName;
        gamesFragment.mPlayerCollaborator = collaborator;
        return gamesFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        gamesList = root.findViewById(R.id.games_list);
        gameDetails = root.findViewById(R.id.game_details_layout);

        team1List = root.findViewById(R.id.game_details_team1);
        team2List = root.findViewById(R.id.game_details_team2);
        setDefaultColors();

        team1List.setOnItemLongClickListener(onPlayerClick);
        team2List.setOnItemLongClickListener(onPlayerClick);

        gamesList.setOnItemClickListener(
                (adapterView, view, position, l) ->
                        onGameSelected((Game) view.getTag(R.id.game)));

        gamesList.setOnItemLongClickListener(
                (adapterView, view, position, l) ->
                        onGameLongClick(((Game) view.getTag(R.id.game))));

        refreshGames();

        return root;
    }

    private void refreshTeams() {
        Context activity = getContext(); if (activity == null) return;
        if (mCurrGameId < 0) return;

        mTeam1 = DbHelper.getCurrTeam(activity, mCurrGameId, TeamEnum.Team1, 0);
        mTeam2 = DbHelper.getCurrTeam(activity, mCurrGameId, TeamEnum.Team2, 0);

        mTeam1.sort(Comparator.comparing(Player::name));
        mTeam2.sort(Comparator.comparing(Player::name));

        team1List.setAdapter(new PlayerTeamGameHistoryAdapter(activity, mTeam1, mPlayerName, mPlayerCollaborator));
        team2List.setAdapter(new PlayerTeamGameHistoryAdapter(activity, mTeam2, mPlayerName, mPlayerCollaborator));
    }

    public void refreshGames() {
        Context activity = getContext(); if (activity == null) return;

        ArrayList<Game> games;
        if (mPlayerName != null && mPlayerCollaborator != null) { // games in which both played
            games = DbHelper.getGames(activity, mPlayerName, mPlayerCollaborator);
        } else if (mPlayerName != null) { // games in which selected player played
            games = DbHelper.getGames(activity, mPlayerName);
        } else { // all games
            games = DbHelper.getGames(activity);
        }

        // Attach cursor adapter to the ListView
        gamesAdapter = new GameAdapter(activity, games, mCurrGameId);
        gamesList.setAdapter(gamesAdapter);
    }

    private void refreshSelectedGame() {
        gamesAdapter.setSelectedGameId(mCurrGameId);
        gamesAdapter.notifyDataSetChanged();
    }

    private void setDefaultColors() {
        int[] colors = ColorHelper.getTeamsColors(getActivity());
        team1List.setBackgroundColor(colors[0]);
        team2List.setBackgroundColor(colors[1]);
    }

    //region game click
    public void onGameSelected(Game game) {
        if (game == null || mCurrGameId == game.gameId) {
            mCurrGameId = -1;
            mCurrGame = null;
            gameDetails.setVisibility(View.GONE);
        } else {
            mCurrGameId = game.gameId;
            mCurrGame = game;
            gameDetails.setVisibility(View.VISIBLE);
        }
        refreshSelectedGame();
        refreshTeams();
    }

    Game getSelectedGame() {
        return mCurrGame;
    }
    //endregion

    //region game long clicked
    private boolean onGameLongClick(Game game) {
        if (mCurrGameId > 0 && mCurrGameId == game.gameId) { // selected game long clicked
            checkCopyGame();
            return true;
        }
        return false;
    }

    private void checkCopyGame() {

        DialogHelper.showApprovalDialog(getContext(), getString(R.string.copy),
                "Copy coming players and teams?",
                ((dialog, which) -> copyGamePlayers()));
    }

    private void copyGamePlayers() {
        FragmentActivity activity = getActivity(); if (activity == null) return;
        DbHelper.clearComingPlayers(activity);
        DbHelper.setPlayerComing(activity, mTeam1);
        DbHelper.setPlayerComing(activity, mTeam2);
        DbHelper.saveTeams(activity, mTeam1, mTeam2);
        Toast.makeText(activity, R.string.copy_players_success, Toast.LENGTH_SHORT).show();
    }
    //endregion

    //region player click
    private AdapterView.OnItemLongClickListener onPlayerClick = (parent, view, position, id) -> {
        Player player = (Player) parent.getItemAtPosition(position);
        checkPlayerChange(player);
        return false;
    };

    private void checkPlayerChange(final Player player) {

        DialogHelper.showApprovalDialog(getContext(),
                "Modify", "Do you want to modify this player attendance?",
                ((dialog, which) -> movePlayer(player))
        );
    }

    private void movePlayer(Player player) {
        Context context = getContext(); if (context == null) return;

        DbHelper.modifyPlayerResult(context, mCurrGameId, player.mName);
        refreshTeams();
    }
    //endregion
}
