package com.teampicker.drorfichman.teampicker.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerParticipation;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.ColorHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drorfichman on 7/30/16.
 */
public class PlayerParticipationAdapter extends ArrayAdapter<PlayerParticipation> {

    private final Context context;
    private final List<PlayerParticipation> mPlayers;
    private final ArrayList<Player> mBlue;
    private final ArrayList<Player> mOrange;
    private final int[] teamsIcons;

    public PlayerParticipationAdapter(Context ctx, List<PlayerParticipation> players,
                                      ArrayList<Player> blue, ArrayList<Player> orange) {
        super(ctx, -1, players);
        context = ctx;
        mPlayers = players;
        mBlue = blue;
        mOrange = orange;

        teamsIcons = ColorHelper.getTeamsIcons(ctx);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.player_participation_item, parent, false);

        TextView name = view.findViewById(R.id.player_name);

        TextView countWith = view.findViewById(R.id.part_games_count_with);
        TextView winRateWith = view.findViewById(R.id.part_wins_percentage_with);

        TextView countVs = view.findViewById(R.id.part_games_count_against);
        TextView winRateVs = view.findViewById(R.id.part_wins_percentage_against);

        PlayerParticipation p = mPlayers.get(position);

        name.setText(p.mName);

        countWith.setText(String.valueOf(p.statisticsWith.gamesCount));
        if (p.statisticsWith.gamesCount > 0)
            winRateWith.setText(context.getString(R.string.player_wins_participation, String.valueOf(p.statisticsWith.successRate), p.statisticsWith.getWinRateDisplay()));
        else
            winRateWith.setText(p.statisticsWith.getWinRateDisplay());

        countVs.setText(String.valueOf(p.statisticsVs.gamesCount));
        if (p.statisticsVs.gamesCount > 0)
            winRateVs.setText(context.getString(R.string.player_wins_participation, String.valueOf(p.statisticsVs.successRate), p.statisticsVs.getWinRateDisplay()));
        else
            winRateVs.setText(p.statisticsVs.getWinRateDisplay());

        ImageView teamIcon = view.findViewById(R.id.team_icon);
        teamIcon.setVisibility(View.INVISIBLE);
        Player player = new Player(p.mName, 0);
        if (mOrange != null && mOrange.contains(player)) {
            teamIcon.setImageResource(teamsIcons[0]);
            teamIcon.setVisibility(View.VISIBLE);
        } else if (mBlue != null && mBlue.contains(player)) {
            teamIcon.setImageResource(teamsIcons[1]);
            teamIcon.setVisibility(View.VISIBLE);
        }

        view.setTag(R.id.player_id, p.mName);

        return view;
    }
}