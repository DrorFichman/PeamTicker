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
    private List<String> mHighlightedNames;

    public PlayerTeamGameHistoryAdapter(Context ctx, List<Player> players, List<String> highlightedNames) {
        super(ctx, -1, players);
        context = ctx;
        mPlayers = players;
        mHighlightedNames = highlightedNames != null ? highlightedNames : new java.util.ArrayList<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = LayoutInflater.from(context).inflate(R.layout.player_team_game_history_item, parent, false);

        Player player = mPlayers.get(position);

        TextView name = rowView.findViewById(R.id.player_team_name);
        setName(player, name);

        ImageView mvpTrophy = rowView.findViewById(R.id.player_mvp_trophy);
        setMvpTrophy(player, mvpTrophy);

        ImageView injuredIcon = rowView.findViewById(R.id.player_injured_icon);
        setInjuredIcon(player, injuredIcon);

        return rowView;
    }

    private void setMvpTrophy(Player player, ImageView mvpTrophy) {
        if (player.gameIsMVP) {
            mvpTrophy.setVisibility(View.VISIBLE);
        } else {
            mvpTrophy.setVisibility(View.GONE);
        }
    }

    private void setInjuredIcon(Player player, ImageView injuredIcon) {
        if (player.gameIsInjured) {
            injuredIcon.setVisibility(View.VISIBLE);
        } else {
            injuredIcon.setVisibility(View.GONE);
        }
    }

    private void setName(Player player, TextView name) {
        name.setText(player.mName + (ResultEnum.Missed.getValue() == player.gameResult ? " **" : ""));

        if (!mHighlightedNames.isEmpty()) {
            name.setAlpha(mHighlightedNames.contains(player.mName) ? 1F : 0.4F);
        } else {
            name.setAlpha(1F);
        }
    }
}