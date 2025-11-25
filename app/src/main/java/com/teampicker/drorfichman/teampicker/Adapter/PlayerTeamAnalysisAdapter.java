package com.teampicker.drorfichman.teampicker.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.Collaboration;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.EffectMargin;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.PlayerCollaboration;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;

import static com.teampicker.drorfichman.teampicker.tools.ColorHelper.setColorAlpha;

/**
 * Created by drorfichman on 7/30/16.
 */
public class PlayerTeamAnalysisAdapter extends ArrayAdapter<Player> {
    public static final int MAX_DELTA_ALPHA = 8;
    public static final int MAX_DELTA_WIN_RATE_ALPHA = 12;

    private Context context;
    private List<Player> mPlayers;
    private List<Player> mMovedPlayers;
    private Collection<Player> mMarkedPlayers;
    private String mSelectedPlayer;
    private Collaboration mCollaboration;

    public PlayerTeamAnalysisAdapter(Context ctx, List<Player> players,
                                     List<Player> coloredPlayers, Collection<Player> markedPlayers,
                                     @NonNull Collaboration collaboration, String selectedPlayer) {
        super(ctx, -1, players);
        context = ctx;
        mPlayers = players;
        mMovedPlayers = coloredPlayers != null ? coloredPlayers : new ArrayList<>();
        mMarkedPlayers = markedPlayers != null ? markedPlayers : new ArrayList<>();
        mCollaboration = collaboration;
        mSelectedPlayer = selectedPlayer;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = LayoutInflater.from(context).inflate(R.layout.player_team_analysis_item, parent, false);

        Player player = mPlayers.get(position);

        TextView name = rowView.findViewById(R.id.player_team_name);
        setName(rowView, player, name);

        TextView games = rowView.findViewById(R.id.player_analysis_games);
        TextView winRate = rowView.findViewById(R.id.player_analysis_win_rate);
        TextView collaboration = rowView.findViewById(R.id.player_analysis_collaboration_win_rate);

        setCollaborationAnalysis(player, games, winRate, collaboration);
        setSelectedPlayer(rowView, player);

        return rowView;
    }

    private void setName(View rowView, Player player, TextView name) {
        name.setText(player.mName.substring(0, Math.min(6, player.mName.length())));

        if (mMarkedPlayers.contains(player)) {
            rowView.setAlpha(0.4F);
        } else if (mMovedPlayers.contains(player)) {
            rowView.setAlpha(0.4F);
        } else {
            rowView.setAlpha(1F);
        }
    }

    private void setSelectedPlayer(View rowView, Player player) {

        if (player.mName.equals(mSelectedPlayer)) {
            rowView.setBackgroundColor(Color.WHITE);
        }
    }

    private void setCollaborationAnalysis(Player player, TextView games, TextView winRate, TextView collaboration) {
        if (mSelectedPlayer != null) { // selected player mode

            PlayerCollaboration selectedPlayerData = mCollaboration.getPlayer(mSelectedPlayer);

            if (player.mName.equals(mSelectedPlayer)) { // selected player stats

                winRate.setText(context.getString(R.string.player_analysis_selected_win_rate, selectedPlayerData.winRate));
                games.setText(context.getString(R.string.player_analysis_selected_games, selectedPlayerData.games));
                collaboration.setText(selectedPlayerData.getExpectedWinRateString());

                setColorAlpha(getContext(), winRate, selectedPlayerData.winRate - 50, MAX_DELTA_WIN_RATE_ALPHA);
                setColorAlpha(getContext(), collaboration, selectedPlayerData.getExpectedWinRateDiff(), MAX_DELTA_ALPHA);

            } else { // collaborator of selected player stats
                EffectMargin collaboratorEffect = selectedPlayerData.getEffect(player.mName);
                PlayerCollaboration collaborator = mCollaboration.getPlayer(player.mName);

                if (collaboratorEffect != null && collaborator != null) { // or - no games together
                    winRate.setText(context.getString(R.string.player_analysis_selected_win_rate, collaborator.winRate));
                    games.setText(context.getString(R.string.player_analysis_selected_games, collaboratorEffect.getGamesWith()));
                    collaboration.setText(context.getString(R.string.player_analysis_selected_win_rate, collaboratorEffect.getWinRateWith()));

                    setColorAlpha(getContext(), winRate, collaborator.winRate - 50, MAX_DELTA_WIN_RATE_ALPHA);
                    setColorAlpha(getContext(), collaboration, collaboratorEffect.getWinRateMarginWith(), MAX_DELTA_ALPHA);
                } else {
                    setEmptyData(winRate, games, collaboration);
                }
            }

        } else { // non-selected player mode
            PlayerCollaboration data = mCollaboration.getPlayer(player.mName);

            if (data != null) {
                winRate.setText(context.getString(R.string.player_analysis_selected_win_rate, data.winRate));
                games.setText(context.getString(R.string.player_analysis_selected_games, data.games));
                collaboration.setText(data.getExpectedWinRateString());

                setColorAlpha(getContext(), winRate, data.winRate - 50, MAX_DELTA_WIN_RATE_ALPHA);
                setColorAlpha(getContext(), collaboration, data.getExpectedWinRate() - data.winRate, MAX_DELTA_ALPHA);
            } else {
                setEmptyData(winRate, games, collaboration);
            }
        }
    }

    private void setEmptyData(TextView winRate, TextView games, TextView collaboration) {
        winRate.setText("-");
        games.setText("-");
        collaboration.setText("-");
        winRate.setAlpha(1);
        games.setAlpha(1);
        collaboration.setAlpha(1);
    }
}