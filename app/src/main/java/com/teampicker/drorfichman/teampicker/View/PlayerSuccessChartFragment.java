package com.teampicker.drorfichman.teampicker.View;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
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
import com.teampicker.drorfichman.teampicker.Data.ResultEnum;
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
import java.util.stream.Collectors;

/**
 * Fragment displaying a line chart of player success (wins - losses) over time
 * for top participating players, with a spinner to select number of players to display.
 */
public class PlayerSuccessChartFragment extends Fragment {

    private static final int MIN_GAMES_FOR_DISPLAY = 30;
    private static final int MIN_GAMES_FOR_PLAYER = 10; // Minimum games for player to show on chart

    private static final int[] PLAYER_COUNT_OPTIONS = {5, 10, 15, 20, 25};

    private LineChart chart;
    private Spinner playerCountSpinner;
    private LinearLayout playerCountSelector;
    private LinearLayout infoPanel;
    private TextView selectedDate;
    private TextView selectedPlayer;
    private TextView selectedValue;
    private TextView emptyMessage;
    private ChipGroup customLegend;
    private ScrollView legendScroll;

    private ArrayList<Game> games;
    private ArrayList<PlayerGame> allPlayerGames;
    private List<String> topPlayerNames;
    private Map<String, List<PlayerSuccessPoint>> playerSuccessData;
    private SimpleDateFormat dateFormat;
    private GameUpdateBroadcast notificationHandler;

    private static final int ALL_PLAYERS = -1;
    private int currentPlayerCount = 10;
    private int highlightedPlayerIndex = -1;  // -1 means no player is highlighted
    private int infoPanelPlayerIndex = -1;    // Track which player is currently shown in info panel
    private List<Chip> legendItems = new ArrayList<>();

    // Distinct colors for up to 25 players
    private static final int[] PLAYER_COLORS = {
            Color.rgb(244, 67, 54),   // Red
            Color.rgb(33, 150, 243),  // Blue
            Color.rgb(76, 175, 80),   // Green
            Color.rgb(255, 152, 0),   // Orange
            Color.rgb(156, 39, 176),  // Purple
            Color.rgb(0, 188, 212),   // Cyan
            Color.rgb(255, 193, 7),   // Amber
            Color.rgb(121, 85, 72),   // Brown
            Color.rgb(233, 30, 99),   // Pink
            Color.rgb(63, 81, 181),   // Indigo
            Color.rgb(139, 195, 74),  // Light Green
            Color.rgb(255, 87, 34),   // Deep Orange
            Color.rgb(103, 58, 183),  // Deep Purple
            Color.rgb(0, 150, 136),   // Teal
            Color.rgb(255, 235, 59),  // Yellow
            Color.rgb(96, 125, 139),  // Blue Grey
            Color.rgb(205, 220, 57),  // Lime
            Color.rgb(158, 158, 158), // Grey
            Color.rgb(244, 143, 177), // Pink Light
            Color.rgb(100, 181, 246), // Blue Light
            Color.rgb(129, 199, 132), // Green Light
            Color.rgb(255, 183, 77),  // Orange Light
            Color.rgb(186, 104, 200), // Purple Light
            Color.rgb(77, 208, 225),  // Cyan Light
            Color.rgb(255, 213, 79),  // Amber Light
    };

    public PlayerSuccessChartFragment() {
        super(R.layout.fragment_player_success_heatmap);
    }

    public static PlayerSuccessChartFragment newInstance() {
        return new PlayerSuccessChartFragment();
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
        chart = root.findViewById(R.id.success_chart);
        playerCountSpinner = root.findViewById(R.id.player_count_spinner);
        playerCountSelector = root.findViewById(R.id.player_count_selector);
        infoPanel = root.findViewById(R.id.info_panel);
        selectedDate = root.findViewById(R.id.selected_date);
        selectedPlayer = root.findViewById(R.id.selected_player);
        selectedValue = root.findViewById(R.id.selected_value);
        emptyMessage = root.findViewById(R.id.empty_message);
        customLegend = root.findViewById(R.id.custom_legend);
        legendScroll = root.findViewById(R.id.legend_scroll);

        setupPlayerCountSpinner();
        loadDataAndSetupChart();

        // Set up info panel click to toggle highlight for the displayed player
        infoPanel.setOnClickListener(v -> {
            if (infoPanelPlayerIndex >= 0) {
                onLegendItemClicked(infoPanelPlayerIndex);
            }
        });

        return root;
    }

    private void setupPlayerCountSpinner() {
        List<String> options = new ArrayList<>();
        for (int count : PLAYER_COUNT_OPTIONS) {
            options.add(String.valueOf(count));
        }
        options.add(getString(R.string.stats_success_all_players));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                options
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerCountSpinner.setAdapter(adapter);

        // Set default selection to 10 players
        int defaultIndex = 1; // Index of "10" in PLAYER_COUNT_OPTIONS
        playerCountSpinner.setSelection(defaultIndex);

        playerCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int newCount;
                if (position < PLAYER_COUNT_OPTIONS.length) {
                    newCount = PLAYER_COUNT_OPTIONS[position];
                } else {
                    newCount = ALL_PLAYERS;  // "All" option
                }
                if (newCount != currentPlayerCount) {
                    // Check if highlighted player will still be visible
                    if (highlightedPlayerIndex != -1) {
                        int effectiveNewCount = (newCount == ALL_PLAYERS)
                                ? (topPlayerNames != null ? topPlayerNames.size() : 0)
                                : newCount;
                        if (highlightedPlayerIndex >= effectiveNewCount) {
                            highlightedPlayerIndex = -1;  // Player no longer visible
                        }
                    }
                    // Check if info panel player will still be visible
                    if (infoPanelPlayerIndex != -1) {
                        int effectiveNewCount = (newCount == ALL_PLAYERS)
                                ? (topPlayerNames != null ? topPlayerNames.size() : 0)
                                : newCount;
                        if (infoPanelPlayerIndex >= effectiveNewCount) {
                            hideInfoPanel();
                        }
                    }
                    currentPlayerCount = newCount;
                    updateChart();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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
                    dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    calculatePlayerSuccessData();
                    if (topPlayerNames == null || topPlayerNames.isEmpty()) {
                        showEmptyState();
                        return;
                    }
                    setupChart();
                    updateChart();
                });
    }

    private void showEmptyState() {
        chart.setVisibility(View.GONE);
        playerCountSelector.setVisibility(View.GONE);
        legendScroll.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(getString(R.string.stats_success_no_data, MIN_GAMES_FOR_DISPLAY));
    }

    private void calculatePlayerSuccessData() {
        // Group player games by game ID
        Map<Integer, List<PlayerGame>> gameIdToPlayers = new HashMap<>();
        Map<String, Integer> playerTotalGames = new HashMap<>();

        for (PlayerGame pg : allPlayerGames) {
            gameIdToPlayers.computeIfAbsent(pg.gameId, k -> new ArrayList<>()).add(pg);
            playerTotalGames.merge(pg.playerName, 1, Integer::sum);
        }

        // Get top players by total games played (with minimum threshold)
        // No limit here - we filter by currentPlayerCount in updateChart()
        topPlayerNames = playerTotalGames.entrySet().stream()
                .filter(e -> e.getValue() >= MIN_GAMES_FOR_PLAYER)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topPlayerNames.isEmpty()) {
            return;
        }

        // Calculate success data for each player
        playerSuccessData = new HashMap<>();

        for (String playerName : topPlayerNames) {
            List<PlayerSuccessPoint> successPoints = new ArrayList<>();
            List<Integer> playerResults = new ArrayList<>();  // 1 = win, 0 = tie, -1 = loss

            for (int gameIdx = 0; gameIdx < games.size(); gameIdx++) {
                Game game = games.get(gameIdx);
                List<PlayerGame> playersInGame = gameIdToPlayers.get(game.gameId);

                if (playersInGame != null) {
                    PlayerGame playerGame = null;
                    for (PlayerGame pg : playersInGame) {
                        if (pg.playerName.equals(playerName)) {
                            playerGame = pg;
                            break;
                        }
                    }

                    if (playerGame != null && playerGame.result != null && ResultEnum.isActive(playerGame.result)) {
                        // Player participated in this game
                        playerResults.add(playerGame.result.getValue());

                        // Calculate cumulative success (sum of all results)
                        if (playerResults.size() >= 3) {
                            int cumulativeSuccess = 0;
                            for (int result : playerResults) {
                                cumulativeSuccess += result;
                            }
                            successPoints.add(new PlayerSuccessPoint(gameIdx, game.getDate(), cumulativeSuccess));
                        }
                    }
                }
            }

            playerSuccessData.put(playerName, successPoints);
        }
    }

    private void setupChart() {
        chart.setVisibility(View.VISIBLE);
        playerCountSelector.setVisibility(View.VISIBLE);
        legendScroll.setVisibility(View.VISIBLE);
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
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setGranularity(1f);
        leftAxis.setTextSize(10f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%+.0f", value);
            }
        });

        // Add reference line at 0 (neutral)
        leftAxis.removeAllLimitLines();
        LimitLine zeroLine = new LimitLine(0f);
        zeroLine.setLineColor(Color.DKGRAY);
        zeroLine.setLineWidth(1.5f);
        leftAxis.addLimitLine(zeroLine);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // Disable built-in legend - we use custom clickable legend
        chart.getLegend().setEnabled(false);

        // Set up selection listener
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int dataSetIndex = h.getDataSetIndex();
                updateInfoPanel((int) e.getX(), e.getY(), dataSetIndex);
            }

            @Override
            public void onNothingSelected() {
                hideInfoPanel();
            }
        });
    }

    private void updateChart() {
        if (playerSuccessData == null || playerSuccessData.isEmpty()) {
            return;
        }

        List<ILineDataSet> dataSets = new ArrayList<>();

        // Create a line for each player (up to currentPlayerCount, or all if ALL_PLAYERS)
        int playerCount = (currentPlayerCount == ALL_PLAYERS) 
                ? topPlayerNames.size() 
                : Math.min(currentPlayerCount, topPlayerNames.size());

        for (int i = 0; i < playerCount; i++) {
            String playerName = topPlayerNames.get(i);
            List<PlayerSuccessPoint> successPoints = playerSuccessData.get(playerName);

            if (successPoints == null || successPoints.isEmpty()) {
                continue;
            }

            ArrayList<Entry> entries = new ArrayList<>();
            for (PlayerSuccessPoint point : successPoints) {
                entries.add(new Entry(point.gameIndex, point.successValue));
            }

            LineDataSet dataSet = new LineDataSet(entries, truncateName(playerName));
            dataSet.setColor(PLAYER_COLORS[i % PLAYER_COLORS.length]);
            dataSet.setLineWidth(2f);
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSet.setMode(LineDataSet.Mode.LINEAR);
            dataSet.setHighLightColor(Color.rgb(255, 193, 7)); // Amber
            dataSet.setHighlightLineWidth(2f);
            dataSet.setDrawVerticalHighlightIndicator(true);
            dataSet.setDrawHorizontalHighlightIndicator(false);

            dataSets.add(dataSet);
        }

        if (dataSets.isEmpty()) {
            return;
        }

        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);

        // Build custom legend
        buildCustomLegend(playerCount);

        // Apply highlight styling if a player is highlighted
        applyHighlightToChart();
    }

    /**
     * Build custom clickable legend below the chart using Chips, sorted alphabetically
     */
    private void buildCustomLegend(int playerCount) {
        customLegend.removeAllViews();
        legendItems.clear();

        // Create list of player indices to display, then sort by name
        int count = Math.min(playerCount, topPlayerNames.size());
        List<Integer> sortedIndices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            sortedIndices.add(i);
        }
        // Sort indices by player name alphabetically
        sortedIndices.sort((a, b) -> topPlayerNames.get(a).compareToIgnoreCase(topPlayerNames.get(b)));

        // Create chips in alphabetical order
        for (int displayIdx = 0; displayIdx < sortedIndices.size(); displayIdx++) {
            int playerIndex = sortedIndices.get(displayIdx);
            String playerName = topPlayerNames.get(playerIndex);
            int color = PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];

            Chip chip = new Chip(requireContext());
            chip.setText(playerName);
            chip.setTextSize(10);
            chip.setChipMinHeight(24 * getResources().getDisplayMetrics().density);
            chip.setChipCornerRadius(12 * getResources().getDisplayMetrics().density);
            chip.setTextStartPadding(6 * getResources().getDisplayMetrics().density);
            chip.setTextEndPadding(6 * getResources().getDisplayMetrics().density);
            chip.setChipStartPadding(0);
            chip.setChipEndPadding(0);
            chip.setChipIconVisible(false);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setSingleLine(true);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            // Style with player color
            chip.setChipStrokeWidth(2 * getResources().getDisplayMetrics().density);
            chip.setChipStrokeColorResource(android.R.color.transparent);
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.WHITE));
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(color));
            chip.setTextColor(color);

            // Store player index (not display index) for click handling
            chip.setTag(playerIndex);
            chip.setOnClickListener(v -> onLegendItemClicked((int) v.getTag()));

            customLegend.addView(chip);
            legendItems.add(chip);
        }

        // Update legend appearance based on current highlight state
        updateLegendAppearance();
    }

    /**
     * Handle legend item click - toggle highlight for the player
     */
    private void onLegendItemClicked(int playerIndex) {
        if (highlightedPlayerIndex == playerIndex) {
            // Clicking same player - deselect
            highlightedPlayerIndex = -1;
        } else {
            // Select new player
            highlightedPlayerIndex = playerIndex;
        }

        // Update chart line appearance
        applyHighlightToChart();

        // Update legend appearance
        updateLegendAppearance();
    }

    /**
     * Apply highlight styling to chart lines
     */
    private void applyHighlightToChart() {
        LineData lineData = chart.getLineData();
        if (lineData == null) return;

        for (int i = 0; i < lineData.getDataSetCount(); i++) {
            LineDataSet dataSet = (LineDataSet) lineData.getDataSetByIndex(i);
            int color = PLAYER_COLORS[i % PLAYER_COLORS.length];

            if (highlightedPlayerIndex == -1) {
                // No highlight - all lines normal
                dataSet.setColor(color);
                dataSet.setLineWidth(2f);
            } else if (i == highlightedPlayerIndex) {
                // This is the highlighted player - make prominent
                dataSet.setColor(color);
                dataSet.setLineWidth(4f);
            } else {
                // Other players - dim them
                dataSet.setColor(applyAlpha(color, 60));  // 60/255 opacity
                dataSet.setLineWidth(1f);
            }
        }

        chart.invalidate();
    }

    /**
     * Update legend items appearance based on highlight state
     */
    private void updateLegendAppearance() {
        float density = getResources().getDisplayMetrics().density;

        for (Chip chip : legendItems) {
            int playerIndex = (int) chip.getTag();
            int color = PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];

            if (highlightedPlayerIndex == -1) {
                // No highlight - all items normal (outlined)
                chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(color));
                chip.setChipStrokeWidth(2 * density);
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.WHITE));
                chip.setTextColor(color);
            } else if (playerIndex == highlightedPlayerIndex) {
                // This is the highlighted player - filled background
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));
                chip.setChipStrokeWidth(0);
                chip.setTextColor(Color.WHITE);
            } else {
                // Other players - dimmed
                chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(applyAlpha(color, 100)));
                chip.setChipStrokeWidth(1 * density);
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.WHITE));
                chip.setTextColor(applyAlpha(color, 100));
            }
        }
    }

    /**
     * Apply alpha to a color
     */
    private int applyAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private String truncateName(String name) {
        if (name.length() > 8) {
            return name.substring(0, 8);
        }
        return name;
    }

    private void updateInfoPanel(int gameIndex, float value, int dataSetIndex) {
        infoPanel.setVisibility(View.VISIBLE);

        // Track the currently displayed player for info panel click handling
        infoPanelPlayerIndex = dataSetIndex;

        // Get date
        if (gameIndex >= 0 && gameIndex < games.size()) {
            Date gameDate = games.get(gameIndex).getDate();
            if (gameDate != null) {
                selectedDate.setText(dateFormat.format(gameDate));
            } else {
                selectedDate.setText(getString(R.string.stats_game_number, gameIndex + 1));
            }
        }

        // Get player name
        if (dataSetIndex >= 0 && dataSetIndex < topPlayerNames.size()) {
            selectedPlayer.setText(topPlayerNames.get(dataSetIndex));
        }

        // Format value
        int intValue = (int) value;
        if (intValue > 0) {
            selectedValue.setText("+" + intValue);
            selectedValue.setTextColor(Color.parseColor("#4CAF50"));  // Green
        } else if (intValue < 0) {
            selectedValue.setText(String.valueOf(intValue));
            selectedValue.setTextColor(Color.parseColor("#F44336"));  // Red
        } else {
            selectedValue.setText("0");
            selectedValue.setTextColor(Color.parseColor("#FFC107"));  // Amber
        }
    }

    private void hideInfoPanel() {
        infoPanel.setVisibility(View.INVISIBLE);
        infoPanelPlayerIndex = -1;
    }

    // Data class for storing player success points
    private static class PlayerSuccessPoint {
        int gameIndex;
        Date date;
        int successValue;

        PlayerSuccessPoint(int gameIndex, Date date, int successValue) {
            this.gameIndex = gameIndex;
            this.date = date;
            this.successValue = successValue;
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
