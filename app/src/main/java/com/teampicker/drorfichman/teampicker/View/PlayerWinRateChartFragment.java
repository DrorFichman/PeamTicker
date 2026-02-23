package com.teampicker.drorfichman.teampicker.View;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import androidx.cardview.widget.CardView;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerGameStat;
import com.teampicker.drorfichman.teampicker.Data.ResultEnum;
import com.teampicker.drorfichman.teampicker.Data.StreakInfo;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.DbAsync;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlayerWinRateChartFragment extends Fragment {

    private static final String ARG_PLAYER = "player";
    private static final int MIN_GAMES_FOR_DISPLAY = 20;

    private Player player;
    private LineChart chart;
    private TextView emptyMessage;
    private CardView unbeatenRunCard;
    private TextView unbeatenRunValue;
    private CardView overallWinRateCard;
    private TextView overallWinRateValue;
    private LinearLayout infoPanel;
    private TextView selectedDate;
    private TextView selectedValue;

    private ArrayList<PlayerGameStat> gameHistory;
    private static final int WINDOW_SIZE = 50;
    private SimpleDateFormat dateFormat;
    
    // Streak highlight fields
    private StreakInfo currentStreak;
    private boolean isStreakHighlighted = false;
    

    public PlayerWinRateChartFragment() {
        super(R.layout.fragment_player_insights);
    }

    public static PlayerWinRateChartFragment newInstance(Player player) {
        PlayerWinRateChartFragment fragment = new PlayerWinRateChartFragment();
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

        assert root != null;
        chart = root.findViewById(R.id.insights_chart);
        emptyMessage = root.findViewById(R.id.insights_empty_message);
        unbeatenRunCard = root.findViewById(R.id.unbeaten_run_card);
        unbeatenRunValue = root.findViewById(R.id.unbeaten_run_value);
        overallWinRateCard = root.findViewById(R.id.overall_win_rate_card);
        overallWinRateValue = root.findViewById(R.id.overall_win_rate_value);
        infoPanel = root.findViewById(R.id.insights_info_panel);
        selectedDate = root.findViewById(R.id.insights_selected_date);
        selectedValue = root.findViewById(R.id.insights_selected_value);

        loadAllData();

        return root;
    }

    /** Bundles all three DB calls into a single background fetch to avoid blocking the main thread. */
    private static class ChartData {
        final ArrayList<PlayerGameStat> gameHistory;
        final StreakInfo streakInfo;
        final Player playerWithStats;

        ChartData(ArrayList<PlayerGameStat> gameHistory, StreakInfo streakInfo, Player playerWithStats) {
            this.gameHistory = gameHistory;
            this.streakInfo = streakInfo;
            this.playerWithStats = playerWithStats;
        }
    }

    private void loadAllData() {
        if (player == null || getContext() == null) {
            showEmptyState();
            return;
        }

        android.content.Context ctx = getContext();
        final String playerName = player.mName;

        DbAsync.run(
                () -> {
                    ArrayList<PlayerGameStat> history = DbHelper.getPlayerLastGames(ctx, player, 1000);
                    StreakInfo streak = DbHelper.getLongestUnbeatenRun(ctx, playerName);
                    Player playerWithStats = DbHelper.getPlayer(ctx, playerName, -1);
                    return new ChartData(history, streak, playerWithStats);
                },
                data -> {
                    if (!isAdded()) return;
                    applyChartData(data);
                });
    }

    private void applyChartData(ChartData data) {
        // --- Game history / chart ---
        if (data.gameHistory == null || data.gameHistory.size() < MIN_GAMES_FOR_DISPLAY) {
            showEmptyState();
        } else {
            gameHistory = data.gameHistory;
            // Games are sorted DESC (newest first); need oldest first for chronological chart
            Collections.reverse(gameHistory);
            dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            setupChart();
            updateChart();
        }

        // --- Longest unbeaten run card ---
        currentStreak = data.streakInfo;
        if (currentStreak != null && currentStreak.length > 0) {
            unbeatenRunValue.setText(String.format("%d games (%d days)", currentStreak.length, currentStreak.days));
            unbeatenRunCard.setVisibility(View.VISIBLE);
            unbeatenRunCard.setOnClickListener(v -> toggleStreakHighlight());
        } else {
            unbeatenRunCard.setVisibility(View.GONE);
        }

        // --- Overall win rate card ---
        Player p = data.playerWithStats;
        if (p != null && p.statistics != null && p.statistics.gamesCount > 0) {
            overallWinRateValue.setText(getString(R.string.win_rate_value, p.statistics.getWinRate()));
            overallWinRateCard.setVisibility(View.VISIBLE);
        } else {
            overallWinRateCard.setVisibility(View.GONE);
        }
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
        
        // Enable highlight by drag - allows swiping through data points
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
        
        // Set up selection listener to update info panel
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) e.getX();
                float winRate = e.getY();
                updateInfoPanel(index, winRate);
            }

            @Override
            public void onNothingSelected() {
                hideInfoPanel();
            }
        });
    }
    
    private void updateInfoPanel(int gameIndex, float winRate) {
        infoPanel.setVisibility(View.VISIBLE);
        
        // Get date from game history
        if (gameIndex >= 0 && gameIndex < gameHistory.size()) {
            Date gameDate = gameHistory.get(gameIndex).getDate();
            if (gameDate != null) {
                selectedDate.setText(dateFormat.format(gameDate));
            } else {
                selectedDate.setText("Game " + (gameIndex + 1));
            }
        } else {
            selectedDate.setText("Game " + (gameIndex + 1));
        }
        
        selectedValue.setText(String.format(Locale.getDefault(), "Win Rate: %.1f%%", winRate));
    }
    
    private void hideInfoPanel() {
        infoPanel.setVisibility(View.INVISIBLE);
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
        dataSet.setHighLightColor(Color.rgb(255, 193, 7)); // Amber highlight
        dataSet.setHighlightLineWidth(2f);
        dataSet.setDrawVerticalHighlightIndicator(true);
        dataSet.setDrawHorizontalHighlightIndicator(false);

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
        dataSet.setHighLightColor(Color.rgb(255, 193, 7)); // Amber highlight
        dataSet.setHighlightLineWidth(2f);
        dataSet.setDrawVerticalHighlightIndicator(true);
        dataSet.setDrawHorizontalHighlightIndicator(false);

        return dataSet;
    }

    private void toggleStreakHighlight() {
        if (gameHistory == null || gameHistory.isEmpty() || currentStreak == null || currentStreak.length == 0) {
            return;
        }
        
        isStreakHighlighted = !isStreakHighlighted;
        
        if (isStreakHighlighted) {
            highlightStreakPeriod();
        } else {
            clearStreakHighlight();
        }
    }
    
    private void highlightStreakPeriod() {
        if (currentStreak.startDate == null || currentStreak.endDate == null) {
            return;
        }
        
        // Find the indices in gameHistory that match the streak dates
        int startIndex = findGameIndexByDate(currentStreak.startDate);
        int endIndex = findGameIndexByDate(currentStreak.endDate);
        
        if (startIndex < 0 || endIndex < 0) {
            return;
        }
        
        // Clear existing limit lines and add new ones for streak boundaries
        XAxis xAxis = chart.getXAxis();
        xAxis.removeAllLimitLines();
        
        // Add vertical limit lines at start and end of streak
        // Position labels at different heights to avoid overlap
        LimitLine startLine = new LimitLine(startIndex, formatDateForDisplay(currentStreak.startDate));
        startLine.setLineColor(Color.rgb(76, 175, 80)); // Green
        startLine.setLineWidth(2f);
        startLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        startLine.setTextSize(10f);
        startLine.setTextColor(Color.rgb(56, 142, 60)); // Darker green
        startLine.enableDashedLine(10f, 5f, 0f);
        xAxis.addLimitLine(startLine);
        
        LimitLine endLine = new LimitLine(endIndex, formatDateForDisplay(currentStreak.endDate));
        endLine.setLineColor(Color.rgb(76, 175, 80)); // Green
        endLine.setLineWidth(2f);
        endLine.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_BOTTOM); // Bottom to avoid overlap
        endLine.setTextSize(10f);
        endLine.setTextColor(Color.rgb(56, 142, 60)); // Darker green
        endLine.enableDashedLine(10f, 5f, 0f);
        xAxis.addLimitLine(endLine);
        
        // Update card appearance to show it's active
        unbeatenRunCard.setCardBackgroundColor(Color.rgb(200, 230, 201)); // Darker green
        
        chart.invalidate();
    }
    
    private void clearStreakHighlight() {
        // Remove limit lines from X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.removeAllLimitLines();
        
        // Rebuild chart without the highlight dataset
        updateChart();
        
        // Reset card appearance
        unbeatenRunCard.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // Original light green
        
        chart.invalidate();
    }
    
    private int findGameIndexByDate(String dateString) {
        if (dateString == null || gameHistory == null) {
            return -1;
        }
        
        for (int i = 0; i < gameHistory.size(); i++) {
            if (dateString.equals(gameHistory.get(i).gameDateString)) {
                return i;
            }
        }
        return -1;
    }
    
    private String formatDateForDisplay(String dateString) {
        if (dateString == null) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return date != null ? outputFormat.format(date) : dateString;
        } catch (Exception e) {
            return dateString;
        }
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

