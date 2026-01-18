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
import com.teampicker.drorfichman.teampicker.Data.PlayerGame;
import com.teampicker.drorfichman.teampicker.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fragment displaying a line chart showing the trend of new players (less than 10 games)
 * over time using a X-game moving average.
 */
public class PlayerSeniorityChartFragment extends Fragment {

    private static final int MIN_GAMES_FOR_DISPLAY = 60;  // Need at least 60 games (skip first)
    private static final int WINDOW_SIZE = 25;
    private static final int NEW_PLAYER_THRESHOLD = 10;  // Players with < 10 games are "new"

    private LineChart chart;
    private TextView emptyMessage;
    private CardView summaryCard;
    private TextView currentAvgValue;
    private LinearLayout infoPanel;
    private TextView selectedDate;
    private TextView selectedValue;

    private ArrayList<Game> games;
    private ArrayList<PlayerGame> allPlayerGames;
    private List<GameNewPlayerData> gameNewPlayerCounts;
    private SimpleDateFormat dateFormat;
    private GameUpdateBroadcast notificationHandler;

    public PlayerSeniorityChartFragment() {
        super(R.layout.fragment_player_seniority_chart);
    }

    public static PlayerSeniorityChartFragment newInstance() {
        return new PlayerSeniorityChartFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationHandler = new GameUpdateBroadcast();
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.GAME_UPDATE_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PULL_DATA_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PLAYER_UPDATE_ACTION, notificationHandler);
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
        chart = root.findViewById(R.id.seniority_chart);
        emptyMessage = root.findViewById(R.id.empty_message);
        summaryCard = root.findViewById(R.id.summary_card);
        currentAvgValue = root.findViewById(R.id.current_avg_value);
        infoPanel = root.findViewById(R.id.info_panel);
        selectedDate = root.findViewById(R.id.selected_date);
        selectedValue = root.findViewById(R.id.selected_value);

        loadDataAndSetupChart();

        return root;
    }

    private void loadDataAndSetupChart() {
        if (getContext() == null) {
            showEmptyState();
            return;
        }

        games = DbHelper.getGames(getContext());
        allPlayerGames = DbHelper.getPlayersGames(getContext());

        if (games == null || games.size() < MIN_GAMES_FOR_DISPLAY) {
            showEmptyState();
            return;
        }

        // Games are sorted DESC (newest first), we need oldest first for chronological chart
        Collections.reverse(games);

        // Calculate new player count for each game
        calculateNewPlayerCounts();

        if (gameNewPlayerCounts == null || gameNewPlayerCounts.size() < WINDOW_SIZE + 10) {
            showEmptyState();
            return;
        }

        // Create date formatter for info panel
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        setupChart();
        updateChart();
        updateSummaryCard();
    }

    private void showEmptyState() {
        chart.setVisibility(View.GONE);
        summaryCard.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(getString(R.string.stats_seniority_no_data, MIN_GAMES_FOR_DISPLAY));
    }

    /**
     * Calculate the number of "new players" (players with < 10 games) for each game.
     */
    private void calculateNewPlayerCounts() {
        gameNewPlayerCounts = new ArrayList<>();

        // Group player games by game ID
        Map<Integer, List<PlayerGame>> gameIdToPlayers = new HashMap<>();
        for (PlayerGame pg : allPlayerGames) {
            gameIdToPlayers.computeIfAbsent(pg.gameId, k -> new ArrayList<>()).add(pg);
        }

        // Track cumulative game count for each player
        Map<String, Integer> playerGameCounts = new HashMap<>();

        // Process games in chronological order
        for (Game game : games) {
            List<PlayerGame> playersInGame = gameIdToPlayers.get(game.gameId);
            if (playersInGame == null || playersInGame.isEmpty()) {
                continue;
            }

            int newPlayerCount = 0;
            int totalPlayers = playersInGame.size();

            // Count how many players in this game are "new" (less than threshold games)
            for (PlayerGame pg : playersInGame) {
                int playerGames = playerGameCounts.getOrDefault(pg.playerName, 0);
                if (playerGames < NEW_PLAYER_THRESHOLD) {
                    newPlayerCount++;
                }
            }

            // Store the data for this game
            gameNewPlayerCounts.add(new GameNewPlayerData(
                    game.gameId,
                    game.getDate(),
                    newPlayerCount,
                    totalPlayers
            ));

            // Increment game count for all participating players
            for (PlayerGame pg : playersInGame) {
                playerGameCounts.merge(pg.playerName, 1, Integer::sum);
            }
        }
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

        // Enable highlight
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
                if (index >= 0 && index < gameNewPlayerCounts.size()) {
                    Date gameDate = gameNewPlayerCounts.get(index).date;
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
                if (gameNewPlayerCounts.size() < 2) return false;
                Date first = gameNewPlayerCounts.get(0).date;
                Date last = gameNewPlayerCounts.get(gameNewPlayerCounts.size() - 1).date;
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
        leftAxis.setTextSize(10f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f", value);
            }
        });

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextSize(11f);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);

        // Set up selection listener
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) e.getX();
                float value = e.getY();
                updateInfoPanel(index, value);
            }

            @Override
            public void onNothingSelected() {
                hideInfoPanel();
            }
        });
    }

    private void updateInfoPanel(int gameIndex, float avgNewPlayers) {
        infoPanel.setVisibility(View.VISIBLE);

        if (gameIndex >= 0 && gameIndex < gameNewPlayerCounts.size()) {
            Date gameDate = gameNewPlayerCounts.get(gameIndex).date;
            if (gameDate != null) {
                selectedDate.setText(dateFormat.format(gameDate));
            } else {
                selectedDate.setText(getString(R.string.stats_game_number, gameIndex + 1));
            }
        } else {
            selectedDate.setText(getString(R.string.stats_game_number, gameIndex + 1));
        }

        selectedValue.setText(getString(R.string.stats_seniority_avg_new_players, avgNewPlayers));
    }

    private void hideInfoPanel() {
        infoPanel.setVisibility(View.INVISIBLE);
    }

    private void updateChart() {
        if (gameNewPlayerCounts == null || gameNewPlayerCounts.isEmpty()) {
            return;
        }

        ArrayList<Entry> entries = new ArrayList<>();

        // Calculate X-game moving average, starting from game X+1
        for (int i = WINDOW_SIZE; i < gameNewPlayerCounts.size(); i++) {
            float movingAvg = calculateMovingAverage(i - WINDOW_SIZE, i);
            entries.add(new Entry(i, movingAvg));
        }

        if (entries.isEmpty()) {
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.stats_seniority_new_players_avg));
        dataSet.setColor(Color.rgb(33, 150, 243)); // Blue
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setHighLightColor(Color.rgb(255, 193, 7)); // Amber
        dataSet.setHighlightLineWidth(2f);
        dataSet.setDrawVerticalHighlightIndicator(true);
        dataSet.setDrawHorizontalHighlightIndicator(false);

        // Add fill
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.rgb(33, 150, 243));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    /**
     * Calculate moving average of new player count for games in range [startIndex, endIndex)
     */
    private float calculateMovingAverage(int startIndex, int endIndex) {
        float total = 0;
        int count = 0;

        for (int i = startIndex; i < endIndex && i < gameNewPlayerCounts.size(); i++) {
            total += gameNewPlayerCounts.get(i).newPlayerCount;
            count++;
        }

        if (count == 0) {
            return 0f;
        }

        return total / count;
    }

    private void updateSummaryCard() {
        if (gameNewPlayerCounts == null || gameNewPlayerCounts.size() < WINDOW_SIZE) {
            summaryCard.setVisibility(View.GONE);
            return;
        }

        // Calculate current average (last X games)
        int lastIndex = gameNewPlayerCounts.size();
        float currentAvg = calculateMovingAverage(lastIndex - WINDOW_SIZE, lastIndex);

        currentAvgValue.setText(getString(R.string.stats_seniority_current_value, currentAvg));
        summaryCard.setVisibility(View.VISIBLE);
    }

    // Data class for storing game new player counts
    private static class GameNewPlayerData {
        int gameId;
        Date date;
        int newPlayerCount;
        int totalPlayers;

        GameNewPlayerData(int gameId, Date date, int newPlayerCount, int totalPlayers) {
            this.gameId = gameId;
            this.date = date;
            this.newPlayerCount = newPlayerCount;
            this.totalPlayers = totalPlayers;
        }
    }

    // Broadcast receiver for updates
    private class GameUpdateBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadDataAndSetupChart();
        }
    }
}

