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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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
import java.util.List;
import java.util.Locale;

public class PlayerInsightsFragment extends Fragment {

    private static final String ARG_PLAYER = "player";
    private static final int MIN_GAMES_FOR_DISPLAY = 20;

    private Player player;
    private LineChart chart;
    private TextView emptyMessage;

    private ArrayList<PlayerGameStat> gameHistory;
    private static final int WINDOW_SIZE = 50;
    private ChartMarkerView markerView;

    public PlayerInsightsFragment() {
        super(R.layout.fragment_player_insights);
    }

    public static PlayerInsightsFragment newInstance(Player player) {
        PlayerInsightsFragment fragment = new PlayerInsightsFragment();
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
        
        chart = root.findViewById(R.id.insights_chart);
        emptyMessage = root.findViewById(R.id.insights_empty_message);

        loadDataAndSetupChart();

        return root;
    }

    private void loadDataAndSetupChart() {
        if (player == null || getContext() == null) {
            showEmptyState();
            return;
        }

        // Fetch all game history for the player (up to 1000 games)
        gameHistory = DbHelper.getPlayerLastGames(getContext(), player, 1000);
        
        if (gameHistory == null || gameHistory.size() < MIN_GAMES_FOR_DISPLAY) {
            showEmptyState();
            return;
        }

        // Games are sorted DESC (newest first), we need oldest first for chronological chart
        Collections.reverse(gameHistory);
        
        // Create marker view for touch data display
        markerView = new ChartMarkerView(requireContext(), gameHistory);
        
        setupChart();
        updateChart();
    }

    private void showEmptyState() {
        chart.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(getString(R.string.insights_no_data, MIN_GAMES_FOR_DISPLAY));
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
        chart.setNoDataText(getString(R.string.insights_no_data, MIN_GAMES_FOR_DISPLAY));
        chart.setBackgroundColor(Color.WHITE);
        chart.setClipValuesToContent(false);
        chart.setClipToPadding(false);
        chart.setExtraRightOffset(30f); // Extra space on the right for marker
        
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
                if (index >= 0 && index < gameHistory.size()) {
                    Date gameDate = gameHistory.get(index).getDate();
                    if (gameDate != null) {
                        // Show year primarily, but include month if there are games spanning short time
                        if (shouldShowMonths()) {
                            return monthYearFormat.format(gameDate);
                        }
                        return yearFormat.format(gameDate);
                    }
                }
                return "";
            }
            
            private boolean shouldShowMonths() {
                if (gameHistory.size() < 2) return false;
                Date first = gameHistory.get(0).getDate();
                Date last = gameHistory.get(gameHistory.size() - 1).getDate();
                if (first == null || last == null) return false;
                
                Calendar cal1 = Calendar.getInstance();
                Calendar cal2 = Calendar.getInstance();
                cal1.setTime(first);
                cal2.setTime(last);
                
                int yearDiff = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR);
                return yearDiff <= 2; // Show months if span is 2 years or less
            }
        });

        // Configure Y-axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setGranularity(10f);
        leftAxis.setTextSize(10f);

        // Add reference lines at 45%, 50%, and 55%
        leftAxis.removeAllLimitLines();
        
        // 45% line - subtle
        LimitLine line45 = new LimitLine(45f);
        line45.setLineColor(Color.GRAY);
        line45.setLineWidth(1f);
        line45.enableDashedLine(10f, 10f, 0f);
        leftAxis.addLimitLine(line45);
        
        // 50% line - highlighted
        LimitLine line50 = new LimitLine(50f);
        line50.setLineColor(Color.DKGRAY);
        line50.setLineWidth(2f);
        leftAxis.addLimitLine(line50);
        
        // 55% line - subtle
        LimitLine line55 = new LimitLine(55f);
        line55.setLineColor(Color.GRAY);
        line55.setLineWidth(1f);
        line55.enableDashedLine(10f, 10f, 0f);
        leftAxis.addLimitLine(line55);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextSize(12f);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);

        // Set marker view for touch data display
        if (markerView != null) {
            chart.setMarker(markerView);
        }
    }

    private void updateChart() {
        if (gameHistory == null || gameHistory.isEmpty()) {
            return;
        }

        List<LineDataSet> dataSets = new ArrayList<>();

        // Add moving win rate line
        LineDataSet movingDataSet = createMovingWinRateDataSet();
        if (movingDataSet != null) {
            dataSets.add(movingDataSet);
        }

        // Add cumulative win rate line (always displayed)
        LineDataSet cumulativeDataSet = createCumulativeWinRateDataSet();
        if (cumulativeDataSet != null) {
            dataSets.add(cumulativeDataSet);
        }

        if (dataSets.isEmpty()) {
            return;
        }

        LineData lineData = new LineData(dataSets.toArray(new LineDataSet[0]));
        chart.setData(lineData);
        chart.invalidate(); // Refresh chart
    }

    private LineDataSet createMovingWinRateDataSet() {
        ArrayList<Entry> entries = new ArrayList<>();

        // Start from MIN_GAMES_FOR_DISPLAY to avoid extreme swings with few games
        for (int i = MIN_GAMES_FOR_DISPLAY - 1; i < gameHistory.size(); i++) {
            int windowStart = Math.max(0, i - WINDOW_SIZE + 1);
            int windowEnd = i + 1;
            
            float winRate = calculateWinRate(windowStart, windowEnd);
            entries.add(new Entry(i, winRate));
        }

        if (entries.isEmpty()) {
            return null;
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.insights_moving_win_rate));
        dataSet.setColor(Color.rgb(76, 175, 80)); // Green
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smoother curve
        dataSet.setCubicIntensity(0.2f);

        return dataSet;
    }

    private LineDataSet createCumulativeWinRateDataSet() {
        ArrayList<Entry> entries = new ArrayList<>();

        // Start from MIN_GAMES_FOR_DISPLAY to avoid extreme swings with few games
        for (int i = MIN_GAMES_FOR_DISPLAY - 1; i < gameHistory.size(); i++) {
            float winRate = calculateWinRate(0, i + 1);
            entries.add(new Entry(i, winRate));
        }

        if (entries.isEmpty()) {
            return null;
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.insights_cumulative_win_rate));
        dataSet.setColor(Color.rgb(33, 150, 243)); // Blue
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smoother curve
        dataSet.setCubicIntensity(0.2f);

        return dataSet;
    }

    /**
     * Calculate win rate for games in range [startIndex, endIndex)
     * Formula: (sum of results + count) / (2 * count) * 100
     * Where: Win = 1, Tie = 0, Lose = -1
     */
    private float calculateWinRate(int startIndex, int endIndex) {
        int resultsSum = 0;
        int count = 0;

        for (int i = startIndex; i < endIndex && i < gameHistory.size(); i++) {
            PlayerGameStat game = gameHistory.get(i);
            if (game.result != null && ResultEnum.isActive(game.result)) {
                resultsSum += game.result.getValue();
                count++;
            }
        }

        if (count == 0) {
            return 0f;
        }

        // Formula: (sum + count) / (2 * count) * 100
        // This converts the -1 to 1 scale to 0 to 100 percentage
        return ((float) (resultsSum + count) / (2f * count)) * 100f;
    }
}

