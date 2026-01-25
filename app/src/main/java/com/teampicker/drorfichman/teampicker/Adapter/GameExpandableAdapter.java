package com.teampicker.drorfichman.teampicker.Adapter;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.TeamEnum;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.ColorHelper;
import com.teampicker.drorfichman.teampicker.tools.DateHelper;
import com.teampicker.drorfichman.teampicker.tools.DialogHelper;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

public class GameExpandableAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final List<Game> mGames;
    private final String mPlayerName;
    private final String mPlayerCollaborator;
    private final GameModified mGameModifiedHandler;
    private final boolean mEditable;
    private final ListView mList;

    public interface GameModified {
        void onGameModified();
    }

    public GameExpandableAdapter(Context ctx, List<Game> games, String playerName, String collaborator,
                                 boolean editable, GameModified handler, ListView list) {
        super();

        context = ctx;
        mGames = games;
        mPlayerName = playerName;
        mPlayerCollaborator = collaborator;
        mGameModifiedHandler = handler;
        mEditable = editable;
        mList = list;
    }

    @Override
    public int getGroupCount() {
        return mGames.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return 1; // just the single game details item
    }

    @Override
    public Game getGroup(int i) {
        return mGames.get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return mGames.get(i);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int position, boolean expanded, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.game_item, null);
        }

        final Game g = getGroup(position);
        TextView dateView = view.findViewById(R.id.game_date);
        TextView playerCountView = view.findViewById(R.id.game_player_count);
        TextView resultSet1 = view.findViewById(R.id.game_result_set1);
        TextView resultSet2 = view.findViewById(R.id.game_result_set2);
        TextView resultDivider = view.findViewById(R.id.game_result_set_divider);
        ImageView res = view.findViewById(R.id.res_1);
        TextView playerGrade = view.findViewById(R.id.game_player_grade);
        ImageView playerMVP = view.findViewById(R.id.game_player_mvp);
        ImageView playerInjured = view.findViewById(R.id.game_player_injured);

        dateView.setText(DateHelper.getDisplayDate(context, g.dateString));
        setResults(g, resultSet1, resultSet2, resultDivider);
        setPlayerCount(g, playerCountView);
        setPlayerResult(res, g);
        setPlayerGrade(playerGrade, g);
        setPlayerMVP(playerMVP, g);
        setPlayerInjured(playerInjured, g);

        view.setTag(R.id.game, g);
        view.setTag(R.id.game_index_id, position);

        view.setBackgroundColor(expanded ? Color.GRAY : Color.TRANSPARENT);
        return view;
    }

    private void setPlayerCount(Game g, TextView playerCountView) {
        ArrayList<Player> team1 = DbHelper.getGameTeam(context, g.gameId, TeamEnum.Team1, 0);
        ArrayList<Player> team2 = DbHelper.getGameTeam(context, g.gameId, TeamEnum.Team2, 0);
        playerCountView.setText(context.getString(R.string.games_player_count, team1.size() + team2.size()));
    }

    @SuppressLint("InflateParams")
    @Override
    public View getChildView(int position, int chilePosition, boolean expanded, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.game_details_child_item, null);
        }

        final Game g = getGroup(position);

        ListView team1 = view.findViewById(R.id.game_details_team1);
        ListView team2 = view.findViewById(R.id.game_details_team2);

        View actionsLayout = view.findViewById(R.id.edit_actions);
        View edit = view.findViewById(R.id.game_edit_game);
        View delete = view.findViewById(R.id.game_delete_game);
        View copy = view.findViewById(R.id.game_copy_game);

        setChildTeams(g.gameId, team1, team2);
        setChildDefaultColors(team1, team2);
        setChildGameActions(g, actionsLayout, edit, delete, copy);

        TutorialManager.userActionTaken(context, TutorialManager.TutorialUserAction.clicked_game_in_history);
        Event.logEvent(FirebaseAnalytics.getInstance(context), EventType.view_game);

        return view;
    }

    private void setChildGameActions(Game g, View actionsLayout, View edit, View delete, View copy) {
        if (!mEditable) {
            actionsLayout.setVisibility(View.GONE);
        } else {
            actionsLayout.setVisibility(View.VISIBLE);
            edit.setOnClickListener(view -> editGame(g));
            delete.setOnClickListener(view -> deleteGame(g));
            copy.setOnClickListener(view -> checkCopyGame(g));
        }
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    private void setResults(Game g, TextView res1, TextView res2, TextView resultDivider) {
        res1.setText(String.valueOf(g.team1Score));
        res2.setText(String.valueOf(g.team2Score));
        resultDivider.setText("-");

        res1.setTypeface(null, g.team1Score > g.team2Score ? Typeface.BOLD : Typeface.NORMAL);
        res2.setTypeface(null, g.team1Score < g.team2Score ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void setPlayerGrade(TextView playerGrade, Game g) {
        if (g.playerGrade > 0) { // player's results - player grade at the time of the game
            playerGrade.setVisibility(View.VISIBLE);
            playerGrade.setText(context.getString(R.string.parentheses, g.playerGrade));
        } else { // game history mode
            playerGrade.setVisibility(View.GONE);
        }
    }

    private void setPlayerMVP(ImageView playerMVP, Game g) {
        if (mPlayerCollaborator == null && g.playerIsMVP) { // player view and was MVP
            playerMVP.setVisibility(View.VISIBLE);
        } else {
            playerMVP.setVisibility(View.INVISIBLE);
        }
    }

    private void setPlayerInjured(ImageView playerInjured, Game g) {
        if (mPlayerCollaborator == null && g.playerIsInjured) { // player view and was injured
            playerInjured.setVisibility(View.VISIBLE);
        } else {
            playerInjured.setVisibility(View.GONE);
        }
    }

    private void setPlayerResult(ImageView starView, Game g) {
        if (g.playerResult != null) { // player's results - green/red
            starView.setImageResource(g.playerResult.getDrawable());
            starView.setVisibility(View.VISIBLE);
        } else if (g.winningTeam != null) { // winning team - orange/blue/tie
            starView.setImageResource(g.winningTeam.getDrawable(context));
            starView.setVisibility(View.VISIBLE);
        } else { // unreachable
            starView.setVisibility(View.GONE);
        }
    }

    private void setChildTeams(int mCurrGameId, ListView team1List, ListView team2List) {
        if (mCurrGameId < 0) return;

        ArrayList<Player> mTeam1 = DbHelper.getGameTeam(context, mCurrGameId, TeamEnum.Team1, 0);
        ArrayList<Player> mTeam2 = DbHelper.getGameTeam(context, mCurrGameId, TeamEnum.Team2, 0);

        mTeam1.sort(Comparator.comparing(Player::name));
        mTeam2.sort(Comparator.comparing(Player::name));

        // Cheat to equalize the height of both lists
        while (mTeam1.size() < mTeam2.size()) {
            mTeam1.add(new Player("", 0));
        }
        while (mTeam2.size() < mTeam1.size()) {
            mTeam2.add(new Player("", 0));
        }

        team1List.setAdapter(new PlayerTeamGameHistoryAdapter(context, mTeam1, mPlayerName, mPlayerCollaborator));
        team2List.setAdapter(new PlayerTeamGameHistoryAdapter(context, mTeam2, mPlayerName, mPlayerCollaborator));
    }

    private void setChildDefaultColors(ListView team1List, ListView team2List) {
        int[] colors = ColorHelper.getTeamsColors(context);
        team1List.setBackgroundColor(colors[0]);
        team2List.setBackgroundColor(colors[1]);
    }

    //region edit games
    // Note: Game editing is intentionally limited to date changes only.
    // Past game attendance (players) cannot be modified to preserve historical integrity.
    public void editGame(Game game) {
        if (game == null) {
            Toast.makeText(context, context.getString(R.string.toast_validation_select_game_edit), Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar date = Calendar.getInstance();
        date.setTime(game.getDate());

        DatePickerDialog d = new DatePickerDialog(context, (datePicker, year, month, day) -> {
            Calendar selectedDate = new Calendar.Builder().setDate(year, month, day).build();
            if (selectedDate.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
                Toast.makeText(context, context.getString(R.string.toast_validation_future_date), Toast.LENGTH_SHORT).show();
            } else {
                updateGameDate(game, DateHelper.getDate(selectedDate.getTimeInMillis()));
                LocalNotifications.sendNotification(context, LocalNotifications.GAME_UPDATE_ACTION);
                Event.logEvent(FirebaseAnalytics.getInstance(context), EventType.edit_game);
            }
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE));
        d.show();
    }

    private void updateGameDate(Game game, String date) {
        DbHelper.updateGameDate(context, game, date);
        if (mGameModifiedHandler != null) mGameModifiedHandler.onGameModified();
        Snackbar.make(context, mList, "Game edited", Snackbar.LENGTH_SHORT).show();
    }

    public void deleteGame(Game game) {
        if (game == null) {
            Toast.makeText(context, context.getString(R.string.toast_validation_select_game_delete), Toast.LENGTH_SHORT).show();
            return;
        }

        DialogHelper.showApprovalDialog(context,
                context.getString(R.string.delete), "Do you want to remove (" + game.getDisplayDate(context) + ")?",
                ((dialog, which) -> {
                    DbHelper.deleteGame(context, game.gameId);
                    if (mGameModifiedHandler != null) mGameModifiedHandler.onGameModified();
                    LocalNotifications.sendNotification(context, LocalNotifications.GAME_UPDATE_ACTION);
                    Snackbar.make(context, mList, "Game deleted", Snackbar.LENGTH_SHORT).show();
                    Event.logEvent(FirebaseAnalytics.getInstance(context), EventType.delete_game);
                })
        );
    }

    private void checkCopyGame(Game g) {
        DialogHelper.showApprovalDialog(context, context.getString(R.string.copy_game),
                "Copy attendance and teams?",
                ((dialog, which) -> copyGamePlayers(g)));
    }

    private void copyGamePlayers(Game g) {
        DbHelper.clearComingPlayers(context);
        ArrayList<Player> mTeam1 = DbHelper.getGameTeam(context, g.gameId, TeamEnum.Team1, 0);
        ArrayList<Player> mTeam2 = DbHelper.getGameTeam(context, g.gameId, TeamEnum.Team2, 0);

        DbHelper.setPlayerComing(context, mTeam1);
        DbHelper.setPlayerComing(context, mTeam2);
        DbHelper.saveTeams(context, mTeam1, mTeam2, null, null, null);
        Snackbar.make(context, mList, context.getString(R.string.copy_players_success), Snackbar.LENGTH_SHORT).show();

        LocalNotifications.sendNotification(context, LocalNotifications.PLAYER_UPDATE_ACTION);
        Event.logEvent(FirebaseAnalytics.getInstance(context), EventType.copy_game);
    }
    //endregion
}
