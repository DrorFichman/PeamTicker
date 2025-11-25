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
import com.teampicker.drorfichman.teampicker.Data.ResultEnum;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by drorfichman on 7/30/16.
 */
public class PlayerTeamGameAdapter extends ArrayAdapter<Player> {
    private Context context;
    private List<Player> mPlayers;
    private List<Player> mMovedPlayers;
    private Collection<Player> mMarkedPlayers;
    private Collection<Player> mMvpPlayers;

    private boolean isAttributesVisible;
    private boolean isGradeVisible;
    private final boolean isRecentGamesVisible;

    public PlayerTeamGameAdapter(Context ctx, List<Player> players,
                                 List<Player> coloredPlayers, Collection<Player> markedPlayers,
                                 Collection<Player> mvpPlayers,
                                 boolean showInternalData, boolean showRecentGames) {
        super(ctx, -1, players);
        context = ctx;
        mPlayers = players;
        mMovedPlayers = coloredPlayers != null ? coloredPlayers : new ArrayList<>();
        mMarkedPlayers = markedPlayers != null ? markedPlayers : new ArrayList<>();
        mMvpPlayers = mvpPlayers != null ? mvpPlayers : new ArrayList<>();

        isGradeVisible = showInternalData;
        isAttributesVisible = showInternalData;
        isRecentGamesVisible = showRecentGames;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = LayoutInflater.from(context).inflate(R.layout.player_team_item, parent, false);

        Player player = mPlayers.get(position);

        TextView name = rowView.findViewById(R.id.player_team_name);
        setName(rowView, player, name);

        setAttributes(rowView, player);

        setGamesHistory(rowView, player);

        TextView grade = rowView.findViewById(R.id.player_team_grade);
        setGrade(player, grade);

        ImageView mvpTrophy = rowView.findViewById(R.id.player_mvp_trophy);
        setMvpTrophy(player, mvpTrophy);

        return rowView;
    }

    private void setMvpTrophy(Player player, ImageView mvpTrophy) {
        if (mMvpPlayers.contains(player)) {
            mvpTrophy.setVisibility(View.VISIBLE);
        } else {
            mvpTrophy.setVisibility(View.GONE);
        }
    }

    private void setName(View rowView, Player player, TextView name) {
        name.setText(player.mName);

        if (mMarkedPlayers.contains(player)) {
            rowView.setAlpha(0.4F);
            rowView.setBackgroundColor(Color.TRANSPARENT);
        } else if (mMovedPlayers.contains(player)) {
            rowView.setBackgroundColor(Color.parseColor("#2ECC71"));
        } else {
            rowView.setAlpha(1F);
            rowView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void setAttributes(View rowView, Player player) {
        rowView.findViewById(R.id.player_gk).setVisibility(isAttributesVisible && player.isGK ? View.VISIBLE : View.GONE);
        rowView.findViewById(R.id.player_d).setVisibility(isAttributesVisible && player.isDefender ? View.VISIBLE : View.GONE);
        rowView.findViewById(R.id.player_pm).setVisibility(isAttributesVisible && player.isPlaymaker ? View.VISIBLE : View.GONE);
        rowView.findViewById(R.id.player_breaking).setVisibility(isAttributesVisible && player.isUnbreakable ? View.VISIBLE : View.GONE);
        rowView.findViewById(R.id.player_extra).setVisibility(isAttributesVisible && player.isExtra ? View.VISIBLE : View.GONE);
        // rowView.findViewById(R.id.player_is_injured).setVisibility(isAttributesVisible && player.isInjured ? View.VISIBLE : View.GONE);
    }

    private void setGrade(Player player, TextView grade) {
        if (isGradeVisible && SettingsHelper.getShowGrades(context)) {
            grade.setText(String.valueOf(player.mGrade));
            grade.setVisibility(View.VISIBLE);
        } else {
            grade.setVisibility(View.GONE);
        }
    }

    private void setGamesHistory(View rowView, Player player) {
        ArrayList<ImageView> starView = new ArrayList();
        starView.add(rowView.findViewById(R.id.res_1));
        starView.add(rowView.findViewById(R.id.res_2));
        starView.add(rowView.findViewById(R.id.res_3));
        starView.add(rowView.findViewById(R.id.res_4));
        starView.add(rowView.findViewById(R.id.res_5));

        for (ImageView im : starView) {
            im.setVisibility(View.INVISIBLE);
        }

        if (isRecentGamesVisible) {
            for (int r = 0; r < player.results.size() && r < starView.size(); ++r) {
                ResultEnum res = player.results.get(r).result;
                boolean isMVP = player.results.get(r).isMVP;
                
                if (res == ResultEnum.Win) {
                    starView.get(r).setImageResource(isMVP ? R.drawable.mvp_star : R.drawable.circle_win);
                } else if (res == ResultEnum.Lose) {
                    starView.get(r).setImageResource(isMVP ? R.drawable.circle_lose_mvp : R.drawable.circle_lose);
                } else if (res == ResultEnum.Tie) {
                    starView.get(r).setImageResource(isMVP ? R.drawable.circle_draw_mvp : R.drawable.circle_draw_blue);
                } else if (res == ResultEnum.Missed) {
                    starView.get(r).setImageResource(R.drawable.circle_na);
                }
                starView.get(r).setVisibility(View.VISIBLE);
            }
        }
    }
}