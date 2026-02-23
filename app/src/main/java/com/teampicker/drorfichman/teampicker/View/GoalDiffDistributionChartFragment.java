package com.teampicker.drorfichman.teampicker.View;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.DbAsync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment displaying a bar chart of goal difference distribution.
 * Shows what percentage of games ended with each score difference.
 */
public class GoalDiffDistributionChartFragment extends Fragment {

    private static final int MIN_GAMES_FOR_DISPLAY = 10;
    private static final int MAX_DIFF_CATEGORY = 4; // 0, 1, 2, 3, 4+

    private BarChart chart;
    private TextView emptyMessage;
    private CardView summaryCard;
    private TextView closeGamesValue;

    private ArrayList<Game> games;
    private GameUpdateBroadcast notificationHandler;

    public GoalDiffDistributionChartFragment() {
        super(R.layout.fragment_goal_diff_distribution_chart);
    }

    public static GoalDiffDistributionChartFragment newInstance() {
        return new GoalDiffDistributionChartFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationHandler = new GameUpdateBroadcast();
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.GAME_UPDATE_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PULL_DATA_ACTION, notificationHandler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalNotifications.unregisterBroadcastReceiver(getContext(), notificationHandler);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        assert root != null;
        chart = root.findViewById(R.id.goal_diff_chart);
        emptyMessage = root.findViewById(R.id.empty_message);
        summaryCard = root.findViewById(R.id.summary_card);
        closeGamesValue = root.findViewById(R.id.close_games_value);

        loadDataAndSetupChart();

        return root;
    }

    private void loadDataAndSetupChart() {
        android.content.Context ctx = getContext();
        if (ctx == null) {
            showEmptyState();
            return;
        }

        DbAsync.run(
                () -> DbHelper.getGames(ctx),
                loadedGames -> {
                    if (!isAdded()) return;
                    if (loadedGames == null || loadedGames.size() < MIN_GAMES_FOR_DISPLAY) {
                        showEmptyState();
                        return;
                    }
                    games = loadedGames;
                    setupChart();
                    updateChart();
                    updateSummaryCard();
                });
    }

    private void showEmptyState() {
        chart.setVisibility(View.GONE);
        summaryCard.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(getString(R.string.charts_no_data, MIN_GAMES_FOR_DISPLAY));
    }

    private void setupChart() {
        chart.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);

        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawBorders(false);
        chart.setBackgroundColor(Color.WHITE);
        chart.setFitBars(true);

        // Configure X-axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int diff = (int) value;
                if (diff == 0) return "Draw";
                if (diff >= MAX_DIFF_CATEGORY) return diff + "+";
                return String.valueOf(diff);
            }
        });

        // Configure Y-axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setTextSize(10f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f%%", value);
            }
        });

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.getLegend().setEnabled(false);
    }

    private void updateChart() {
        if (games == null || games.isEmpty()) {
            return;
        }

        // Calculate goal difference distribution
        Map<Integer, Integer> diffCounts = new HashMap<>();
        for (int i = 0; i <= MAX_DIFF_CATEGORY; i++) {
            diffCounts.put(i, 0);
        }

        for (Game game : games) {
            int diff = Math.abs(game.team1Score - game.team2Score);
            int category = Math.min(diff, MAX_DIFF_CATEGORY);
            diffCounts.put(category, diffCounts.get(category) + 1);
        }

        // Convert to percentages and create entries
        ArrayList<BarEntry> entries = new ArrayList<>();
        int totalGames = games.size();

        for (int i = 0; i <= MAX_DIFF_CATEGORY; i++) {
            float percentage = (diffCounts.get(i) * 100f) / totalGames;
            entries.add(new BarEntry(i, percentage));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Goal Difference");
        
        // Colors: Green for competitive (< 3), Red for non-competitive (>= 3)
        int[] colors = new int[]{
                Color.rgb(76, 175, 80),   // 0 - Draw - Green (competitive)
                Color.rgb(76, 175, 80),   // 1 - Green (competitive)
                Color.rgb(76, 175, 80),   // 2 - Green (competitive)
                Color.rgb(244, 67, 54),   // 3 - Red (not competitive)
                Color.rgb(244, 67, 54)    // 4+ - Red (not competitive)
        };
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value < 1) return "";
                return String.format(Locale.getDefault(), "%.1f%%", value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        chart.setData(barData);
        chart.invalidate();
    }

    private void updateSummaryCard() {
        if (games == null || games.isEmpty()) {
            summaryCard.setVisibility(View.GONE);
            return;
        }

        // Calculate percentage of competitive games (diff < 3)
        int competitiveGames = 0;
        for (Game game : games) {
            int diff = Math.abs(game.team1Score - game.team2Score);
            if (diff < 3) {
                competitiveGames++;
            }
        }

        float competitivePercentage = (competitiveGames * 100f) / games.size();
        closeGamesValue.setText(String.format(Locale.getDefault(), "%.0f%% of games", competitivePercentage));
        summaryCard.setVisibility(View.VISIBLE);
    }

    // Broadcast receiver for game updates
    private class GameUpdateBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadDataAndSetupChart();
        }
    }
}

