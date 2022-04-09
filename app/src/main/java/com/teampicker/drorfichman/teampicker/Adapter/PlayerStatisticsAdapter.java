package com.teampicker.drorfichman.teampicker.Adapter;

import static com.teampicker.drorfichman.teampicker.tools.ColorHelper.setColorAlpha;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.teampicker.drorfichman.teampicker.Controller.Search.FilterView;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;

import java.util.List;

/**
 * Created by drorfichman on 7/30/16.
 */
public class PlayerStatisticsAdapter extends ArrayAdapter<Player> {

    private final Context context;
    private final List<Player> mPlayers;
    private final int totalGamesCount;
    private final boolean isGradeVisible;
    private String filterName;

    int maxSuccess = 0;
    int maxGames = 0;

    public PlayerStatisticsAdapter(Context ctx, List<Player> players, int gamesCount, boolean showGrades) {
        super(ctx, -1, players);
        context = ctx;
        mPlayers = players;
        isGradeVisible = showGrades;
        totalGamesCount = gamesCount;

        for (Player p : players) {
            if (Math.abs(p.statistics.successRate) > maxSuccess) {
                maxSuccess = p.statistics.successRate;
            }
            if (p.statistics.gamesCount > maxGames) {
                maxGames = p.statistics.gamesCount;
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.player_statistics_item, parent, false);

        TextView nameView = view.findViewById(R.id.player_name);
        TextView gradeView = view.findViewById(R.id.stat_player_grade);
        TextView gamesCountView = view.findViewById(R.id.stat_games_count);
        TextView successView = view.findViewById(R.id.stat_success);
        TextView winRateView = view.findViewById(R.id.stat_wins_percentage);

        Player p = mPlayers.get(position);

        nameView.setText(p.mName);
        view.setBackgroundColor(FilterView.match(p.mName, filterName) ? Color.GRAY : Color.TRANSPARENT);

        if (isGradeVisible && SettingsHelper.getShowGrades(context)) {
            gradeView.setText(String.valueOf(p.mGrade));
        } else {
            gradeView.setVisibility(View.INVISIBLE);
        }

        if (p.statistics != null) {
            successView.setText(String.valueOf(p.statistics.successRate));
            setColorAlpha(context, successView, p.statistics.successRate, maxSuccess);

            gamesCountView.setText(p.statistics.getGamesPercentageDisplay(context, totalGamesCount));
            setColorAlpha(context, gamesCountView, p.statistics.getGamesPercentage(totalGamesCount));

            winRateView.setText(String.valueOf(p.statistics.getWinRateDisplay()));
            setColorAlpha(context, winRateView, p.statistics.getWinRate());
        }

        view.setTag(R.id.player_id, p.mName);

        return view;
    }

    public void setFilter(String name) {
        filterName = name;
        notifyDataSetChanged();
    }

    public int positionOfFirstFilterItem(FilterView.onFilterNoResults  handler) {
        return FilterView.positionOfFirstFilterItem(mPlayers, filterName, handler);
    }
}