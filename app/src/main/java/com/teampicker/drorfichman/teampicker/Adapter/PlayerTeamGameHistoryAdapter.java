package com.teampicker.drorfichman.teampicker.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.ResultEnum;
import com.teampicker.drorfichman.teampicker.R;

import java.util.List;

/**
 * Created by drorfichman on 7/30/16.
 */
public class PlayerTeamGameHistoryAdapter extends ArrayAdapter<Player> {
    private Context context;
    private List<Player> mPlayers;
    private String mSelectedPlayer;
    private String mCollaborator;

    public PlayerTeamGameHistoryAdapter(Context ctx, List<Player> players, String selectedPlayer, String mPlayerCollaborator) {
        super(ctx, -1, players);
        context = ctx;
        mPlayers = players;
        mSelectedPlayer = selectedPlayer;
        mCollaborator = mPlayerCollaborator;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = LayoutInflater.from(context).inflate(R.layout.player_team_game_history_item, parent, false);

        Player player = mPlayers.get(position);

        TextView name = rowView.findViewById(R.id.player_team_name);
        setName(player, name);

        ImageView mvpTrophy = rowView.findViewById(R.id.player_mvp_trophy);
        setMvpTrophy(player, mvpTrophy);

        return rowView;
    }

    private void setMvpTrophy(Player player, ImageView mvpTrophy) {
        if (player.gameIsMVP) {
            mvpTrophy.setVisibility(View.VISIBLE);
        } else {
            mvpTrophy.setVisibility(View.GONE);
        }
    }

    private void setName(Player player, TextView name) {
        name.setText(player.mName + (ResultEnum.Missed.getValue() == player.gameResult ? " **" : ""));

        if (mSelectedPlayer != null)
            name.setAlpha(player.mName.equals(mSelectedPlayer) || player.mName.equals(mCollaborator) ? 1F : 0.4F);
        else
            name.setAlpha(1F);
    }
}