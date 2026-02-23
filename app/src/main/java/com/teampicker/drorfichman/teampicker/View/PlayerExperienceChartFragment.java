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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.PlayerGame;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.DbAsync;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment displaying a stacked area chart showing player experience distribution over time.
 * Three categories: New (<10 games), Returning (10-50 games), Regulars (>50 games).
 */
public class PlayerExperienceChartFragment extends Fragment {

    private static final int MIN_GAMES_FOR_DISPLAY = 60;
    private static final int WINDOW_SIZE = 25;
    private static final int NEW_PLAYER_THRESHOLD = 10;      // < 10 games
    private static final int RETURNING_PLAYER_THRESHOLD = 50; // 10-50 games
    // > 50 games = Regulars

    // Colors for each category
    private static final int COLOR_NEW = Color.rgb(76, 175, 80);       // Green #4CAF50
    private static final int COLOR_RETURNING = Color.rgb(33, 150, 243); // Blue #2196F3
    private static final int COLOR_REGULARS = Color.rgb(255, 152, 0);   // Orange #FF9800

    private LineChart chart;
    private TextView emptyMessage;
    private CardView summaryCard;
    private TextView currentAvgValue;
    private LinearLayout infoPanel;
    private TextView clickHint;
    private TextView selectedDate;
    private LinearLayout playerColumns;
    private TextView newPlayersList;
    private TextView returningPlayersList;
    private TextView regularsList;

    private ArrayList<Game> games;
    private ArrayList<PlayerGame> allPlayerGames;
    private List<GameExperienceData> gameExperienceData;
    private Map<String, Integer> playerEventualTotalGames;
    private SimpleDateFormat dateFormat;
    private GameUpdateBroadcast notificationHandler;

    public PlayerExperienceChartFragment() {
        super(R.layout.fragment_player_seniority_chart);
    }

    public static PlayerExperienceChartFragment newInstance() {
        return new PlayerExperienceChartFragment();
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
        clickHint = root.findViewById(R.id.click_hint);
        selectedDate = root.findViewById(R.id.selected_date);
        playerColumns = root.findViewById(R.id.player_columns);
        newPlayersList = root.findViewById(R.id.new_players_list);
        returningPlayersList = root.findViewById(R.id.returning_players_list);
        regularsList = root.findViewById(R.id.regulars_list);

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
                () -> new ArrayList[]{DbHelper.getGames(ctx), DbHelper.getPlayersGames(ctx)},
                raw -> {
                    if (!isAdded()) return;
                    @SuppressWarnings("unchecked")
                    ArrayList<Game> loadedGames = (ArrayList<Game>) raw[0];
                    @SuppressWarnings("unchecked")
                    ArrayList<PlayerGame> loadedPlayerGames = (ArrayList<PlayerGame>) raw[1];

                    if (loadedGames == null || loadedGames.size() < MIN_GAMES_FOR_DISPLAY) {
                        showEmptyState();
                        return;
                    }
                    games = loadedGames;
                    allPlayerGames = loadedPlayerGames;

                    // Games are sorted DESC (newest first); need oldest first for chronological chart
                    Collections.reverse(games);
                    computeEventualTotalGames();
                    calculateExperienceDistribution();

                    if (gameExperienceData == null || gameExperienceData.size() < WINDOW_SIZE + 10) {
                        showEmptyState();
                        return;
                    }
                    dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    setupChart();
                    updateChart();
                    updateSummaryCard();
                });
    }

    private void showEmptyState() {
        chart.setVisibility(View.GONE);
        summaryCard.setVisibility(View.GONE);
        infoPanel.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(getString(R.string.stats_seniority_no_data, MIN_GAMES_FOR_DISPLAY));
    }

    /**
     * First pass: Count total games each player will eventually play.
     */
    private void computeEventualTotalGames() {
        playerEventualTotalGames = new HashMap<>();

        // Group player games by game ID
        Map<Integer, List<PlayerGame>> gameIdToPlayers = new HashMap<>();
        for (PlayerGame pg : allPlayerGames) {
            gameIdToPlayers.computeIfAbsent(pg.gameId, k -> new ArrayList<>()).add(pg);
        }

        // Count total games for each player across all games
        for (Game game : games) {
            List<PlayerGame> playersInGame = gameIdToPlayers.get(game.gameId);
            if (playersInGame == null) continue;

            for (PlayerGame pg : playersInGame) {
                playerEventualTotalGames.merge(pg.playerName, 1, Integer::sum);
            }
        }
    }

    /**
     * Second pass: For each game, categorize players and store distribution.
     */
    private void calculateExperienceDistribution() {
        gameExperienceData = new ArrayList<>();

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

            List<String> newPlayers = new ArrayList<>();
            List<String> returningPlayers = new ArrayList<>();
            List<String> regulars = new ArrayList<>();

            // Categorize players based on their game count AT THIS POINT
            for (PlayerGame pg : playersInGame) {
                int playerGames = playerGameCounts.getOrDefault(pg.playerName, 0);
                
                if (playerGames < NEW_PLAYER_THRESHOLD) {
                    newPlayers.add(pg.playerName);
                } else if (playerGames < RETURNING_PLAYER_THRESHOLD) {
                    returningPlayers.add(pg.playerName);
                } else {
                    regulars.add(pg.playerName);
                }
            }

            // Store the data for this game
            gameExperienceData.add(new GameExperienceData(
                    game.gameId,
                    game.getDate(),
                    newPlayers,
                    returningPlayers,
                    regulars
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
        infoPanel.setVisibility(View.VISIBLE);

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
                if (index >= 0 && index < gameExperienceData.size()) {
                    Date gameDate = gameExperienceData.get(index).date;
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
                if (gameExperienceData.size() < 2) return false;
                Date first = gameExperienceData.get(0).date;
                Date last = gameExperienceData.get(gameExperienceData.size() - 1).date;
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
                return String.format(Locale.getDefault(), "%.0f", value);
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
                updateInfoPanel(index);
            }

            @Override
            public void onNothingSelected() {
                showClickHint();
            }
        });
    }

    private void updateInfoPanel(int gameIndex) {
        if (gameIndex < 0 || gameIndex >= gameExperienceData.size()) {
            showClickHint();
            return;
        }

        GameExperienceData data = gameExperienceData.get(gameIndex);

        // Hide click hint, show player data
        clickHint.setVisibility(View.GONE);
        selectedDate.setVisibility(View.VISIBLE);
        playerColumns.setVisibility(View.VISIBLE);

        // Set date
        if (data.date != null) {
            selectedDate.setText(dateFormat.format(data.date));
        } else {
            selectedDate.setText(getString(R.string.stats_game_number, gameIndex + 1));
        }

        // Get all players from each category sorted by eventual total games
        newPlayersList.setText(formatPlayerList(data.newPlayers));
        returningPlayersList.setText(formatPlayerList(data.returningPlayers));
        regularsList.setText(formatPlayerList(data.regulars));
    }

    private String formatPlayerList(List<String> players) {
        if (players == null || players.isEmpty()) {
            return "-";
        }

        // Sort by eventual total games (descending)
        List<String> sorted = new ArrayList<>(players);
        sorted.sort((p1, p2) -> {
            int total1 = playerEventualTotalGames.getOrDefault(p1, 0);
            int total2 = playerEventualTotalGames.getOrDefault(p2, 0);
            return Integer.compare(total2, total1);
        });

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            String player = sorted.get(i);
            int totalGames = playerEventualTotalGames.getOrDefault(player, 0);
            sb.append(getString(R.string.stats_seniority_player_games, player, totalGames));
            if (i < sorted.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void showClickHint() {
        clickHint.setVisibility(View.VISIBLE);
        selectedDate.setVisibility(View.GONE);
        playerColumns.setVisibility(View.GONE);
    }

    private void updateChart() {
        if (gameExperienceData == null || gameExperienceData.isEmpty()) {
            return;
        }

        ArrayList<Entry> regularsEntries = new ArrayList<>();
        ArrayList<Entry> returningEntries = new ArrayList<>();
        ArrayList<Entry> newEntries = new ArrayList<>();

        // Calculate moving averages for stacked display
        for (int i = WINDOW_SIZE; i < gameExperienceData.size(); i++) {
            float avgNew = calculateMovingAverage(i - WINDOW_SIZE, i, Category.NEW);
            float avgReturning = calculateMovingAverage(i - WINDOW_SIZE, i, Category.RETURNING);
            float avgRegulars = calculateMovingAverage(i - WINDOW_SIZE, i, Category.REGULARS);

            // Stack the values: regulars at bottom, returning in middle, new on top
            regularsEntries.add(new Entry(i, avgRegulars));
            returningEntries.add(new Entry(i, avgRegulars + avgReturning));
            newEntries.add(new Entry(i, avgRegulars + avgReturning + avgNew));
        }

        if (regularsEntries.isEmpty()) {
            return;
        }

        // Create datasets in reverse order for proper stacking (bottom to top)
        // New players (top layer - drawn last, needs to be added first for fill to work)
        LineDataSet newDataSet = createStackedDataSet(newEntries, 
                getString(R.string.stats_seniority_new_players), COLOR_NEW);
        
        // Returning players (middle layer)
        LineDataSet returningDataSet = createStackedDataSet(returningEntries, 
                getString(R.string.stats_seniority_returning_players), COLOR_RETURNING);
        
        // Regulars (bottom layer)
        LineDataSet regularsDataSet = createStackedDataSet(regularsEntries, 
                getString(R.string.stats_seniority_regulars), COLOR_REGULARS);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(newDataSet);
        dataSets.add(returningDataSet);
        dataSets.add(regularsDataSet);

        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);
        chart.invalidate();
    }

    private LineDataSet createStackedDataSet(ArrayList<Entry> entries, String label, int color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
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
        dataSet.setFillColor(color);
        dataSet.setFillAlpha(180);

        return dataSet;
    }

    private enum Category { NEW, RETURNING, REGULARS }

    /**
     * Calculate moving average of player count for games in range [startIndex, endIndex)
     */
    private float calculateMovingAverage(int startIndex, int endIndex, Category category) {
        float total = 0;
        int count = 0;

        for (int i = startIndex; i < endIndex && i < gameExperienceData.size(); i++) {
            GameExperienceData data = gameExperienceData.get(i);
            switch (category) {
                case NEW:
                    total += data.newPlayers.size();
                    break;
                case RETURNING:
                    total += data.returningPlayers.size();
                    break;
                case REGULARS:
                    total += data.regulars.size();
                    break;
            }
            count++;
        }

        if (count == 0) {
            return 0f;
        }

        return total / count;
    }

    private void updateSummaryCard() {
        if (gameExperienceData == null || gameExperienceData.size() < WINDOW_SIZE) {
            summaryCard.setVisibility(View.GONE);
            return;
        }

        // Calculate current averages (last X games)
        int lastIndex = gameExperienceData.size();
        float avgNew = calculateMovingAverage(lastIndex - WINDOW_SIZE, lastIndex, Category.NEW);
        float avgReturning = calculateMovingAverage(lastIndex - WINDOW_SIZE, lastIndex, Category.RETURNING);
        float avgRegulars = calculateMovingAverage(lastIndex - WINDOW_SIZE, lastIndex, Category.REGULARS);

        currentAvgValue.setText(getString(R.string.stats_seniority_current_value, avgNew, avgReturning, avgRegulars));
        summaryCard.setVisibility(View.VISIBLE);
    }

    // Data class for storing game experience distribution
    private static class GameExperienceData {
        int gameId;
        Date date;
        List<String> newPlayers;
        List<String> returningPlayers;
        List<String> regulars;

        GameExperienceData(int gameId, Date date, List<String> newPlayers, 
                          List<String> returningPlayers, List<String> regulars) {
            this.gameId = gameId;
            this.date = date;
            this.newPlayers = newPlayers;
            this.returningPlayers = returningPlayers;
            this.regulars = regulars;
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
