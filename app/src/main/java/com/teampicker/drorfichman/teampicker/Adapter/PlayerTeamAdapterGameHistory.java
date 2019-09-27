package com.teampicker.drorfichman.teampicker.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.teampicker.drorfichman.teampicker.Controller.CollaborationHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.ResultEnum;
import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drorfichman on 7/30/16.
 */
public class PlayerTeamAdapterGameHistory extends ArrayAdapter<Player> {
    private Context context;
    private List<Player> mPlayers;
    private List<Player> mMarkedPlayers;

    public PlayerTeamAdapterGameHistory(Context ctx, List<Player> players, List<Player> markedPlayers) {
        super(ctx, -1, players);
        context = ctx;
        mPlayers = players;
        mMarkedPlayers = markedPlayers != null ? markedPlayers : new ArrayList<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = LayoutInflater.from(context).inflate(R.layout.player_team_item_game_history, parent, false);

        Player player = mPlayers.get(position);

        TextView name = rowView.findViewById(R.id.player_team_name);
        setName(player, name);

        return rowView;
    }

    private void setName(Player player, TextView name) {
        name.setText(player.mName + (mMarkedPlayers.contains(player) ? " **" : ""));
    }
}