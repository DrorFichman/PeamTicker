package com.teampicker.drorfichman.teampicker.View;

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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerGameStat;
import com.teampicker.drorfichman.teampicker.Data.ResultEnum;
import com.teampicker.drorfichman.teampicker.Data.StreakInfo;
import com.teampicker.drorfichman.teampicker.R;

import com.github.mikephil.charting.components.LimitLine;

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
 * Line chart showing player participation rate over time (quarterly).
 * X-axis: Time periods (quarters)
 * Y-axis: Participation percentage (player games / total games that quarter)
 * Markers show win/loss/tie breakdown on tap.
 */
public class PlayerParticipationChartFragment extends Fragment {

    private static final String ARG_PLAYER = "player";
    private static final int MIN_GAMES_FOR_DISPLAY = 10;

    private Player player;
    private LineChart chart;
    private TextView emptyMessage;
    private TextView chartTitle;
    private CardView consecutiveAttendanceCard;
    private TextView consecutiveAttendanceValue;

    private ArrayList<PlayerGameStat> gameHistory;
    private ArrayList<Game> allGames;
    private List<ParticipationMarkerView.QuarterData> quarterDataList;
    
    // Streak highlight fields
    private StreakInfo currentStreak;
    private boolean isStreakHighlighted = false;

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
        consecutiveAttendanceCard = root.findViewById(R.id.consecutive_attendance_card);
        consecutiveAttendanceValue = root.findViewById(R.id.consecutive_attendance_value);

        loadDataAndSetupChart();
        updateConsecutiveAttendance();

        return root;
    }

    private void loadDataAndSetupChart() {
        if (player == null || getContext() == null) {
            showEmptyState();
            return;
        }

        // Fetch player's game history
        gameHistory = DbHelper.getPlayerLastGames(getContext(), player, 1000);

        if (gameHistory == null || gameHistory.size() < MIN_GAMES_FOR_DISPLAY) {
            showEmptyState();
            return;
        }

        // Fetch all games to calculate total games per quarter
        allGames = DbHelper.getGames(getContext());

        if (allGames == null || allGames.isEmpty()) {
            showEmptyState();
            return;
        }

        // Games are sorted DESC (newest first), we need oldest first for chronological chart
        Collections.reverse(gameHistory);
        Collections.reverse(allGames);

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
        chart.setClipValuesToContent(false);
        chart.setClipToPadding(false);
        chart.setExtraTopOffset(70f);  // Extra space for marker at 100%
        chart.setExtraRightOffset(30f);
        chart.setExtraBottomOffset(15f);

        // Configure X-axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setTextSize(10f);

        // Configure Y-axis (percentage 0-100)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setGranularity(10f);
        leftAxis.setTextSize(10f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f%%", value);
            }
        });

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // Configure legend
        chart.getLegend().setEnabled(false);
    }

    private void updateChart() {
        if (gameHistory == null || gameHistory.isEmpty() || allGames == null || allGames.isEmpty()) {
            return;
        }

        // Build quarter data with participation rates
        quarterDataList = buildQuarterData();

        if (quarterDataList.isEmpty()) {
            showEmptyState();
            return;
        }

        // Create line entries
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < quarterDataList.size(); i++) {
            ParticipationMarkerView.QuarterData data = quarterDataList.get(i);
            entries.add(new Entry(i, data.participationRate));
        }

        // Create smooth line data set
        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.insights_participation_title));
        dataSet.setColor(Color.rgb(33, 150, 243)); // Blue
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.rgb(33, 150, 243));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve
        dataSet.setCubicIntensity(0.2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.rgb(33, 150, 243));
        dataSet.setFillAlpha(30);
        dataSet.setHighLightColor(Color.rgb(255, 193, 7)); // Amber highlight
        dataSet.setHighlightLineWidth(2f);

        // Set X-axis labels
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < quarterDataList.size()) {
                    return quarterDataList.get(idx).periodLabel;
                }
                return "";
            }
        });
        chart.getXAxis().setLabelCount(Math.min(quarterDataList.size(), 10), false);

        // Set marker view
        ParticipationMarkerView markerView = new ParticipationMarkerView(requireContext(), quarterDataList);
        chart.setMarker(markerView);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    /**
     * Build quarter data with player stats and total games per quarter.
     * Includes all quarters from the first game to the last, even if player didn't participate.
     */
    private List<ParticipationMarkerView.QuarterData> buildQuarterData() {
        List<ParticipationMarkerView.QuarterData> result = new ArrayList<>();

        // Find the date range from all games
        Date firstGameDate = allGames.get(0).getDate();
        Date lastGameDate = allGames.get(allGames.size() - 1).getDate();

        if (firstGameDate == null || lastGameDate == null) {
            return result;
        }

        // Build a map of all quarters with total game counts
        Map<String, Integer> totalGamesPerQuarter = new LinkedHashMap<>();
        for (Game game : allGames) {
            Date gameDate = game.getDate();
            if (gameDate == null) continue;

            String quarterKey = getQuarterKey(gameDate);
            totalGamesPerQuarter.merge(quarterKey, 1, Integer::sum);
        }

        // Build player stats per quarter
        Map<String, int[]> playerStatsPerQuarter = new LinkedHashMap<>();
        for (PlayerGameStat game : gameHistory) {
            Date gameDate = game.getDate();
            if (gameDate == null) continue;

            String quarterKey = getQuarterKey(gameDate);
            // [0] = total player games, [1] = wins, [2] = ties, [3] = losses
            int[] stats = playerStatsPerQuarter.computeIfAbsent(quarterKey, k -> new int[4]);
            stats[0]++;

            if (game.result != null && ResultEnum.isActive(game.result)) {
                int resultValue = game.result.getValue();
                if (resultValue > 0) {
                    stats[1]++; // Win
                } else if (resultValue < 0) {
                    stats[3]++; // Loss
                } else {
                    stats[2]++; // Tie
                }
            }
        }

        // Generate all quarters in range, including those with no player participation
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(firstGameDate);
        int startYear = startCal.get(Calendar.YEAR);
        int startQuarter = (startCal.get(Calendar.MONTH) / 3) + 1;

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(lastGameDate);
        int endYear = endCal.get(Calendar.YEAR);
        int endQuarter = (endCal.get(Calendar.MONTH) / 3) + 1;

        int year = startYear;
        int quarter = startQuarter;

        while (year < endYear || (year == endYear && quarter <= endQuarter)) {
            String quarterKey = "Q" + quarter + " " + String.format(Locale.getDefault(), "%02d", year % 100);
            String displayLabel = "Q" + quarter + " '" + String.format(Locale.getDefault(), "%02d", year % 100);

            int totalGames = totalGamesPerQuarter.getOrDefault(quarterKey, 0);
            int[] playerStats = playerStatsPerQuarter.getOrDefault(quarterKey, new int[4]);

            // Only include quarters where games were actually played
            if (totalGames > 0) {
                result.add(new ParticipationMarkerView.QuarterData(
                        displayLabel,
                        playerStats[0], // player games
                        totalGames,
                        playerStats[1], // wins
                        playerStats[2], // ties
                        playerStats[3]  // losses
                ));
            }

            // Move to next quarter
            quarter++;
            if (quarter > 4) {
                quarter = 1;
                year++;
            }
        }

        return result;
    }

    /**
     * Get quarter key string from date (e.g., "Q1 24" for January 2024)
     */
    private String getQuarterKey(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int quarter = (cal.get(Calendar.MONTH) / 3) + 1;
        int year = cal.get(Calendar.YEAR) % 100;
        return "Q" + quarter + " " + String.format(Locale.getDefault(), "%02d", year);
    }

    private void updateConsecutiveAttendance() {
        if (player != null && getContext() != null) {
            currentStreak = DbHelper.getConsecutiveAttendance(getContext(), player.mName);
            if (currentStreak.length > 0) {
                consecutiveAttendanceValue.setText(String.format("%d games (%d days period)", currentStreak.length, currentStreak.days));
                consecutiveAttendanceCard.setVisibility(View.VISIBLE);
                
                // Add click listener to highlight the streak period on the chart
                consecutiveAttendanceCard.setOnClickListener(v -> toggleStreakHighlight());
            } else {
                consecutiveAttendanceCard.setVisibility(View.GONE);
            }
        }
    }
    
    private void toggleStreakHighlight() {
        if (quarterDataList == null || quarterDataList.isEmpty() || currentStreak == null || currentStreak.length == 0) {
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
        
        // Find the quarter indices that contain the streak dates
        int startIndex = findQuarterIndexByDate(currentStreak.startDate);
        int endIndex = findQuarterIndexByDate(currentStreak.endDate);
        
        if (startIndex < 0 || endIndex < 0) {
            return;
        }
        
        // Clear existing limit lines and add new ones for streak boundaries
        XAxis xAxis = chart.getXAxis();
        xAxis.removeAllLimitLines();
        
        // Add vertical limit lines at start and end of streak
        LimitLine startLine = new LimitLine(startIndex, formatDateForDisplay(currentStreak.startDate));
        startLine.setLineColor(Color.rgb(33, 150, 243)); // Blue
        startLine.setLineWidth(2f);
        startLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        startLine.setTextSize(10f);
        startLine.setTextColor(Color.rgb(25, 118, 210)); // Darker blue
        startLine.enableDashedLine(10f, 5f, 0f);
        xAxis.addLimitLine(startLine);
        
        LimitLine endLine = new LimitLine(endIndex, formatDateForDisplay(currentStreak.endDate));
        endLine.setLineColor(Color.rgb(33, 150, 243)); // Blue
        endLine.setLineWidth(2f);
        endLine.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
        endLine.setTextSize(10f);
        endLine.setTextColor(Color.rgb(25, 118, 210)); // Darker blue
        endLine.enableDashedLine(10f, 5f, 0f);
        xAxis.addLimitLine(endLine);
        
        // Add filled area dataset for the streak period
        addStreakHighlightDataset(startIndex, endIndex);
        
        // Update card appearance to show it's active
        consecutiveAttendanceCard.setCardBackgroundColor(Color.rgb(187, 222, 251)); // Darker blue
        
        chart.invalidate();
    }
    
    private void clearStreakHighlight() {
        // Remove limit lines from X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.removeAllLimitLines();
        
        // Rebuild chart without the highlight dataset
        updateChart();
        
        // Reset card appearance
        consecutiveAttendanceCard.setCardBackgroundColor(Color.parseColor("#E3F2FD")); // Original light blue
        
        chart.invalidate();
    }
    
    private int findQuarterIndexByDate(String dateString) {
        if (dateString == null || quarterDataList == null) {
            return -1;
        }
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            if (date == null) return -1;
            
            String quarterKey = getQuarterDisplayLabel(date);
            
            for (int i = 0; i < quarterDataList.size(); i++) {
                if (quarterKey.equals(quarterDataList.get(i).periodLabel)) {
                    return i;
                }
            }
        } catch (Exception e) {
            // Fall through
        }
        return -1;
    }
    
    private String getQuarterDisplayLabel(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int quarter = (cal.get(Calendar.MONTH) / 3) + 1;
        int year = cal.get(Calendar.YEAR) % 100;
        return "Q" + quarter + " '" + String.format(Locale.getDefault(), "%02d", year);
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
    
    private void addStreakHighlightDataset(int startIndex, int endIndex) {
        if (chart.getData() == null) {
            return;
        }
        
        // Create entries for the highlight area (full height of chart)
        ArrayList<Entry> highlightEntries = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            highlightEntries.add(new Entry(i, 100f)); // Top of chart
        }
        
        if (highlightEntries.isEmpty()) {
            return;
        }
        
        LineDataSet highlightDataSet = new LineDataSet(highlightEntries, "Attendance Streak");
        highlightDataSet.setColor(Color.TRANSPARENT);
        highlightDataSet.setDrawCircles(false);
        highlightDataSet.setDrawValues(false);
        highlightDataSet.setDrawFilled(true);
        highlightDataSet.setFillColor(Color.rgb(33, 150, 243)); // Blue
        highlightDataSet.setFillAlpha(50); // Semi-transparent
        highlightDataSet.setHighlightEnabled(false);
        
        // Get existing data and add the highlight dataset
        LineData lineData = chart.getData();
        lineData.addDataSet(highlightDataSet);
        
        chart.notifyDataSetChanged();
    }
}
