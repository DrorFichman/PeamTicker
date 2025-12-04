package com.teampicker.drorfichman.teampicker.View;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerGameStat;
import com.teampicker.drorfichman.teampicker.Data.ResultEnum;
import com.teampicker.drorfichman.teampicker.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stacked bar chart showing player participation over time.
 * X-axis: Time periods (months or quarters)
 * Y-axis: Game count
 * Bars are stacked showing wins (green), ties (gray), losses (red).
 */
public class PlayerParticipationChartFragment extends Fragment {

    private static final String ARG_PLAYER = "player";
    private static final int MIN_GAMES_FOR_DISPLAY = 10;

    private Player player;
    private BarChart chart;
    private TextView emptyMessage;
    private TextView chartTitle;

    private ArrayList<PlayerGameStat> gameHistory;
    private List<String> periodLabels;

    public PlayerParticipationChartFragment() {
        super(R.layout.fragment_player_participation_chart);
    }

    public static PlayerParticipationChartFragment newInstance(Player player) {
        PlayerParticipationChartFragment fragment = new PlayerParticipationChartFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLAYER, player);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            player = (Player) getArguments().getSerializable(ARG_PLAYER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        chart = root.findViewById(R.id.participation_chart);
        emptyMessage = root.findViewById(R.id.participation_empty_message);
        chartTitle = root.findViewById(R.id.participation_chart_title);

        loadDataAndSetupChart();

        return root;
    }

    private void loadDataAndSetupChart() {
        if (player == null || getContext() == null) {
            showEmptyState();
            return;
        }

        // Fetch all game history for the player
        gameHistory = DbHelper.getPlayerLastGames(getContext(), player, 1000);

        if (gameHistory == null || gameHistory.size() < MIN_GAMES_FOR_DISPLAY) {
            showEmptyState();
            return;
        }

        // Games are sorted DESC (newest first), we need oldest first for chronological chart
        Collections.reverse(gameHistory);

        setupChart();
        updateChart();
    }

    private void showEmptyState() {
        chart.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(getString(R.string.insights_participation_no_data, MIN_GAMES_FOR_DISPLAY));
    }

    private void setupChart() {
        chart.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);

        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawBorders(true);
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(false);

        // Configure X-axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setTextSize(10f);

        // Configure Y-axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextSize(10f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // Configure legend
        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextSize(11f);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);
    }

    private void updateChart() {
        if (gameHistory == null || gameHistory.isEmpty()) {
            return;
        }

        // Determine if we should use months or quarters based on time span
        boolean useQuarters = shouldUseQuarters();

        // Group games by period
        Map<String, int[]> periodStats = groupGamesByPeriod(useQuarters);

        if (periodStats.isEmpty()) {
            showEmptyState();
            return;
        }

        // Create bar entries
        ArrayList<BarEntry> entries = new ArrayList<>();
        periodLabels = new ArrayList<>(periodStats.keySet());

        int index = 0;
        for (String period : periodLabels) {
            int[] stats = periodStats.get(period);
            // stats[0] = wins, stats[1] = ties, stats[2] = losses
            entries.add(new BarEntry(index, new float[]{stats[0], stats[1], stats[2]}));
            index++;
        }

        // Create stacked bar data set
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(
                Color.rgb(76, 175, 80),   // Green for wins
                Color.rgb(158, 158, 158), // Gray for ties
                Color.rgb(244, 67, 54)    // Red for losses
        );
        dataSet.setStackLabels(new String[]{
                getString(R.string.insights_wins_label),
                getString(R.string.insights_ties_label),
                getString(R.string.insights_losses_label)
        });
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);

        // Set X-axis labels
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < periodLabels.size()) {
                    return periodLabels.get(idx);
                }
                return "";
            }
        });
        chart.getXAxis().setLabelCount(Math.min(periodLabels.size(), 12), false);

        chart.setData(barData);
        chart.setFitBars(true);
        chart.invalidate();
    }

    private boolean shouldUseQuarters() {
        if (gameHistory.size() < 2) return false;

        Date first = gameHistory.get(0).getDate();
        Date last = gameHistory.get(gameHistory.size() - 1).getDate();
        if (first == null || last == null) return false;

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(first);
        cal2.setTime(last);

        int yearDiff = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR);
        int monthDiff = yearDiff * 12 + (cal2.get(Calendar.MONTH) - cal1.get(Calendar.MONTH));

        // Use quarters if span is more than 18 months
        return monthDiff > 18;
    }

    private Map<String, int[]> groupGamesByPeriod(boolean useQuarters) {
        // LinkedHashMap to preserve insertion order
        Map<String, int[]> periodStats = new LinkedHashMap<>();

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yy", Locale.getDefault());

        for (PlayerGameStat game : gameHistory) {
            Date gameDate = game.getDate();
            if (gameDate == null) continue;

            String periodKey;
            if (useQuarters) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(gameDate);
                int quarter = (cal.get(Calendar.MONTH) / 3) + 1;
                int year = cal.get(Calendar.YEAR) % 100;
                periodKey = "Q" + quarter + " " + String.format(Locale.getDefault(), "%02d", year);
            } else {
                periodKey = monthFormat.format(gameDate);
            }

            int[] stats = periodStats.computeIfAbsent(periodKey, k -> new int[3]);

            if (game.result != null && ResultEnum.isActive(game.result)) {
                int resultValue = game.result.getValue();
                if (resultValue > 0) {
                    stats[0]++; // Win
                } else if (resultValue < 0) {
                    stats[2]++; // Loss
                } else {
                    stats[1]++; // Tie
                }
            }
        }

        return periodStats;
    }
}

