package com.teampicker.drorfichman.teampicker.View;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.DbAsync;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment displaying a line chart of moving average goal difference over time.
 * Lower values indicate more competitive/balanced games.
 */
public class GoalDiffTimeChartFragment extends Fragment {

    private static final int MIN_GAMES_FOR_DISPLAY = 10;
    private static final int WINDOW_SIZE_SMALL = 10;
    private static final int WINDOW_SIZE_LARGE = 50;

    private LineChart chart;
    private TextView emptyMessage;
    private CardView summaryCard;
    private TextView avgGoalDiffValue;
    private LinearLayout infoPanel;
    private TextView selectedDate;
    private TextView selectedValue;

    private ArrayList<Game> games;
    private SimpleDateFormat dateFormat;
    private GameUpdateBroadcast notificationHandler;

    public GoalDiffTimeChartFragment() {
        super(R.layout.fragment_goal_diff_median_chart);
    }

    public static GoalDiffTimeChartFragment newInstance() {
        return new GoalDiffTimeChartFragment();
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
        chart = root.findViewById(R.id.goal_diff_trend_chart);
        emptyMessage = root.findViewById(R.id.empty_message);
        summaryCard = root.findViewById(R.id.trend_summary_card);
        avgGoalDiffValue = root.findViewById(R.id.avg_goal_diff_value);
        infoPanel = root.findViewById(R.id.info_panel);
        selectedDate = root.findViewById(R.id.selected_date);
        selectedValue = root.findViewById(R.id.selected_value);

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
                    // Games are sorted DESC (newest first); need oldest first for chronological chart
                    Collections.reverse(games);
                    dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
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
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawBorders(true);
        chart.setBackgroundColor(Color.WHITE);
        chart.setClipValuesToContent(false);
        chart.setClipToPadding(false);

        // Enable highlight by drag
        chart.setHighlightPerDragEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        // Configure X-axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setTextSize(10f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
            private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < games.size()) {
                    Date gameDate = games.get(index).getDate();
                    if (gameDate != null) {
                        if (shouldShowMonths()) {
                            return monthYearFormat.format(gameDate);
                        }
                        return yearFormat.format(gameDate);
                    }
                }
                return "";
            }

            private boolean shouldShowMonths() {
                if (games.size() < 2) return false;
                Date first = games.get(0).getDate();
                Date last = games.get(games.size() - 1).getDate();
                if (first == null || last == null) return false;

                Calendar cal1 = Calendar.getInstance();
                Calendar cal2 = Calendar.getInstance();
                cal1.setTime(first);
                cal2.setTime(last);

                int yearDiff = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR);
                return yearDiff <= 2;
            }
        });

        // Configure Y-axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setGranularity(1f);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setTextSize(10f);

        // Add reference line at 3.0 (competitive threshold)
        leftAxis.removeAllLimitLines();
        LimitLine competitiveLine = new LimitLine(3.0f, "Competitive (< 3 goals)");
        competitiveLine.setLineColor(Color.rgb(156, 39, 176));  // Purple
        competitiveLine.setLineWidth(2f);
        competitiveLine.enableDashedLine(10f, 5f, 0f);
        competitiveLine.setTextSize(10f);
        competitiveLine.setTextColor(Color.rgb(156, 39, 176));  // Purple
        competitiveLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        leftAxis.addLimitLine(competitiveLine);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextSize(11f);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);

        // Set up selection listener to update info panel
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) e.getX();
                float medianDiff = e.getY();
                updateInfoPanel(index, medianDiff);
            }

            @Override
            public void onNothingSelected() {
                hideInfoPanel();
            }
        });
    }

    private void updateInfoPanel(int gameIndex, float medianDiff) {
        infoPanel.setVisibility(View.VISIBLE);

        if (gameIndex >= 0 && gameIndex < games.size()) {
            Date gameDate = games.get(gameIndex).getDate();
            if (gameDate != null) {
                selectedDate.setText(dateFormat.format(gameDate));
            } else {
                selectedDate.setText("Game " + (gameIndex + 1));
            }
        } else {
            selectedDate.setText("Game " + (gameIndex + 1));
        }

        selectedValue.setText(String.format(Locale.getDefault(), "Median Diff: %.1f", medianDiff));
    }

    private void hideInfoPanel() {
        infoPanel.setVisibility(View.INVISIBLE);
    }

    private void updateChart() {
        if (games == null || games.isEmpty()) {
            return;
        }

        List<LineDataSet> dataSets = new ArrayList<>();

        // Last 10 games average
        LineDataSet avg10Set = createMovingDataSet(WINDOW_SIZE_SMALL);
        if (avg10Set != null) {
            avg10Set.setColor(Color.rgb(33, 150, 243)); // Blue
            avg10Set.setLabel(getString(R.string.charts_avg_last_n_games, WINDOW_SIZE_SMALL));
            dataSets.add(avg10Set);
        }

        // Only show 50-game line if we have enough games
        if (games.size() >= WINDOW_SIZE_LARGE) {
            // Last 50 games average
            LineDataSet avg50Set = createMovingDataSet(WINDOW_SIZE_LARGE);
            if (avg50Set != null) {
                avg50Set.setColor(Color.rgb(76, 175, 80)); // Green
                avg50Set.setLabel(getString(R.string.charts_avg_last_n_games, WINDOW_SIZE_LARGE));
                dataSets.add(avg50Set);
            }
        }

        if (dataSets.isEmpty()) {
            return;
        }

        LineData lineData = new LineData(dataSets.toArray(new LineDataSet[0]));
        chart.setData(lineData);
        chart.invalidate();
    }

    /**
     * Create a moving average or median dataset
     *
     * @param windowSize The window size for calculation
     */
    private LineDataSet createMovingDataSet(int windowSize) {
        ArrayList<Entry> entries = new ArrayList<>();

        // Start from windowSize to have enough data
        for (int i = windowSize - 1; i < games.size(); i++) {
            float value = calculateAverageDiff(i - windowSize + 1, i + 1);
            entries.add(new Entry(i, value));
        }

        if (entries.isEmpty()) {
            return null;
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setHighLightColor(Color.rgb(255, 193, 7));
        dataSet.setHighlightLineWidth(2f);
        dataSet.setDrawVerticalHighlightIndicator(true);
        dataSet.setDrawHorizontalHighlightIndicator(false);

        return dataSet;
    }

    /**
     * Calculate average goal difference for games in range [startIndex, endIndex)
     */
    private float calculateAverageDiff(int startIndex, int endIndex) {
        int totalDiff = 0;
        int count = 0;

        for (int i = startIndex; i < endIndex && i < games.size(); i++) {
            Game game = games.get(i);
            totalDiff += Math.abs(game.team1Score - game.team2Score);
            count++;
        }

        if (count == 0) {
            return 0f;
        }

        return (float) totalDiff / count;
    }

    private void updateSummaryCard() {
        if (games == null || games.isEmpty()) {
            summaryCard.setVisibility(View.GONE);
            return;
        }

        // Calculate overall average goal difference
        int totalDiff = 0;
        for (Game game : games) {
            totalDiff += Math.abs(game.team1Score - game.team2Score);
        }

        float avgDiff = (float) totalDiff / games.size();
        avgGoalDiffValue.setText(String.format(Locale.getDefault(), "%.1f goals", avgDiff));
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

