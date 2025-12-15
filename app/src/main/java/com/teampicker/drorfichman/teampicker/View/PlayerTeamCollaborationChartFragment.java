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

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import android.view.MotionEvent;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.teampicker.drorfichman.teampicker.Data.BuilderPlayerCollaborationStatistics;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerChemistry;
import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scatter chart showing player collaboration in the context of current teams.
 * For teammates: shows success WITH (games played together)
 * For opponents: shows success AGAINST (games played against each other)
 * X-axis: Number of games
 * Y-axis: Success rate (wins - losses)
 * Color: Team color based on which team the player belongs to
 */
public class PlayerTeamCollaborationChartFragment extends Fragment {

    private static final String ARG_PLAYER = "player";
    private static final String ARG_PLAYER_TEAM = "player_team";
    private static final String ARG_OPPOSING_TEAM = "opposing_team";
    private static final String ARG_RECENT_GAMES = "recent_games";
    private static final int MIN_GAMES_THRESHOLD = 3;
    private static final float MARKER_SIZE = 50f;  // Fixed pixel size for all markers
    private static final float HIGHLIGHT_SIZE = 80f;  // Size of highlight ring around selected dot
    private static final int EXTREME_LABELS_COUNT = 5;  // Show labels only for top/bottom N players
    private static final float SHOW_ALL_LABELS_THRESHOLD = 15f;  // Show all labels when X range is this or smaller

    private Player player;
    private ScatterChart chart;
    private TextView emptyMessage;
    private TextView chartTitle;
    private LinearLayout infoPanel;
    private TextView selectedName;
    private TextView selectedStats;
    
    // Team data
    private Set<String> playerTeamNames;
    private Set<String> opposingTeamNames;
    
    // Number of recent games to use for statistics
    private int recentGames = 50;
    
    // Player's overall success rate for the horizontal reference line
    private int playerOverallSuccess = 0;
    
    // Data for marker view
    private List<TeamCollaborationEntry> allEntries;
    
    // Indices of entries that should show labels (extreme values)
    private Set<Integer> extremeIndices;
    
    // Highlight dataset for showing selection ring
    private ScatterDataSet highlightDataSet;
    
    // Track whether all labels are currently shown
    private boolean showingAllLabels = false;
    
    // Track currently highlighted entry to restore after chart rebuild
    private TeamCollaborationEntry highlightedEntry = null;

    public PlayerTeamCollaborationChartFragment() {
        super(R.layout.fragment_player_team_collaboration_chart);
    }

    /**
     * Data class to hold collaboration entry info for the marker view
     */
    static class TeamCollaborationEntry {
        String playerName;
        int games;
        int success;
        boolean isTeammate;
        
        TeamCollaborationEntry(String name, int games, int success, boolean isTeammate) {
            this.playerName = name;
            this.games = games;
            this.success = success;
            this.isTeammate = isTeammate;
        }
    }

    /**
     * Creates a new instance for showing team collaboration.
     * 
     * @param player The player whose collaboration to show
     * @param playerTeam Players on the same team as the clicked player
     * @param opposingTeam Players on the opposing team
     */
    public static PlayerTeamCollaborationChartFragment newInstance(Player player,
            ArrayList<Player> playerTeam, ArrayList<Player> opposingTeam, int recentGames) {
        PlayerTeamCollaborationChartFragment fragment = new PlayerTeamCollaborationChartFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLAYER, player);
        args.putSerializable(ARG_PLAYER_TEAM, playerTeam);
        args.putSerializable(ARG_OPPOSING_TEAM, opposingTeam);
        args.putInt(ARG_RECENT_GAMES, recentGames);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            player = (Player) getArguments().getSerializable(ARG_PLAYER);
            recentGames = getArguments().getInt(ARG_RECENT_GAMES, 50);
            
            @SuppressWarnings("unchecked")
            ArrayList<Player> playerTeam = (ArrayList<Player>) getArguments().getSerializable(ARG_PLAYER_TEAM);
            @SuppressWarnings("unchecked")
            ArrayList<Player> opposingTeam = (ArrayList<Player>) getArguments().getSerializable(ARG_OPPOSING_TEAM);
            
            playerTeamNames = new HashSet<>();
            if (playerTeam != null) {
                for (Player p : playerTeam) {
                    playerTeamNames.add(p.mName);
                }
            }
            
            opposingTeamNames = new HashSet<>();
            if (opposingTeam != null) {
                for (Player p : opposingTeam) {
                    opposingTeamNames.add(p.mName);
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        assert root != null;
        chart = root.findViewById(R.id.team_collaboration_chart);
        emptyMessage = root.findViewById(R.id.team_collaboration_empty_message);
        chartTitle = root.findViewById(R.id.team_collaboration_chart_title);
        infoPanel = root.findViewById(R.id.team_collaboration_info_panel);
        selectedName = root.findViewById(R.id.team_collaboration_selected_name);
        selectedStats = root.findViewById(R.id.team_collaboration_selected_stats);

        loadDataAndSetupChart();

        return root;
    }

    private void loadDataAndSetupChart() {
        if (player == null || getContext() == null) {
            showEmptyState("No player data available");
            return;
        }

        // Load player with statistics to get overall success rate (using recentGames)
        Player playerWithStats = DbHelper.getPlayer(getContext(), player.mName, recentGames);
        if (playerWithStats != null && playerWithStats.statistics != null) {
            playerOverallSuccess = playerWithStats.statistics.successRate;
        }

        // Set title with number of games
        chartTitle.setText(getString(R.string.team_collaboration_chart_title_with_games, player.mName, recentGames));

        // Get collaboration statistics for recent games (same as used for team division)
        HashMap<String, PlayerChemistry> collaborations = DbHelper.getPlayersParticipationStatistics(
                getContext(), player.mName,
                new BuilderPlayerCollaborationStatistics().setGames(recentGames));

        if (collaborations == null || collaborations.isEmpty()) {
            showEmptyState("No collaboration data available");
            return;
        }

        // Build entries list - only include players from current teams
        allEntries = new ArrayList<>();
        
        for (PlayerChemistry pc : collaborations.values()) {
            // Skip the player themselves
            if (pc.mName.equals(player.mName)) {
                continue;
            }
            
            boolean isTeammate = playerTeamNames.contains(pc.mName);
            boolean isOpponent = opposingTeamNames.contains(pc.mName);
            
            if (isTeammate) {
                // For teammates: use "with" statistics
                if (pc.statisticsWith.gamesCount >= MIN_GAMES_THRESHOLD) {
                    allEntries.add(new TeamCollaborationEntry(
                            pc.mName,
                            pc.statisticsWith.gamesCount,
                            pc.statisticsWith.successRate,
                            true));
                }
            } else if (isOpponent) {
                // For opponents: use "against" statistics
                if (pc.statisticsVs.gamesCount >= MIN_GAMES_THRESHOLD) {
                    allEntries.add(new TeamCollaborationEntry(
                            pc.mName,
                            pc.statisticsVs.gamesCount,
                            pc.statisticsVs.successRate,
                            false));
                }
            }
        }

        if (allEntries.isEmpty()) {
            showEmptyState(getString(R.string.team_collaboration_no_data, MIN_GAMES_THRESHOLD));
            return;
        }

        setupChart();
        updateChart();
    }

    private void showEmptyState(String message) {
        chart.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(message);
    }

    private void setupChart() {
        chart.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);

        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawBorders(true);
        chart.setBackgroundColor(Color.WHITE);
        chart.setExtraBottomOffset(15f);

        // Configure X-axis (Number of games)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setTextSize(10f);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);
        
        // Use chart description for X-axis label (positioned at bottom)
        chart.getDescription().setEnabled(true);
        chart.getDescription().setText(getString(R.string.team_collaboration_x_axis_label));
        chart.getDescription().setTextSize(12f);
        chart.getDescription().setTextColor(Color.DKGRAY);

        // Configure Y-axis (Success rate)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setTextSize(10f);
        leftAxis.setDrawAxisLine(true);

        // Add horizontal line at 0 (neutral success)
        LimitLine yZeroLine = new LimitLine(0f);
        yZeroLine.setLineColor(Color.DKGRAY);
        yZeroLine.setLineWidth(1f);
        leftAxis.addLimitLine(yZeroLine);
        
        // Add horizontal line showing player's overall success rate
        LimitLine playerSuccessLine = new LimitLine(playerOverallSuccess, player.mName + " recent success");
        playerSuccessLine.setLineColor(Color.parseColor("#9C27B0")); // Purple color to stand out
        playerSuccessLine.setLineWidth(2f);
        playerSuccessLine.enableDashedLine(10f, 5f, 0f);
        playerSuccessLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        playerSuccessLine.setTextSize(10f);
        playerSuccessLine.setTextColor(Color.parseColor("#9C27B0"));
        leftAxis.addLimitLine(playerSuccessLine);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // Hide legend - colors are self-explanatory from team context
        chart.getLegend().setEnabled(false);

        // Enable highlight by drag - allows swiping through dots
        chart.setHighlightPerDragEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        
        // Set up selection listener to update info panel and highlight ring
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                Object data = e.getData();
                if (data != null) {
                    int index = (int) data;
                    if (index >= 0 && index < allEntries.size()) {
                        TeamCollaborationEntry entry = allEntries.get(index);
                        highlightedEntry = entry;
                        updateInfoPanel(entry);
                        updateHighlightRing(entry);
                    }
                }
            }

            @Override
            public void onNothingSelected() {
                highlightedEntry = null;
                hideInfoPanel();
                clearHighlightRing();
            }
        });
        
        // Set up gesture listener to show/hide labels based on zoom level
        chart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                updateLabelsVisibility();
            }

            @Override
            public void onChartLongPressed(MotionEvent me) {}

            @Override
            public void onChartDoubleTapped(MotionEvent me) {
                chart.postDelayed(() -> updateLabelsVisibility(), 100);
            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {}

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {}

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                updateLabelsVisibility();
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {}
        });
    }
    
    private void updateLabelsVisibility() {
        if (chart == null || chart.getData() == null) return;
        
        float visibleXRange = chart.getVisibleXRange();
        boolean shouldShowAllLabels = visibleXRange <= SHOW_ALL_LABELS_THRESHOLD;
        
        if (showingAllLabels != shouldShowAllLabels) {
            showingAllLabels = shouldShowAllLabels;
            // Rebuild chart to update label visibility
            updateChart();
        }
    }
    
    private void updateHighlightRing(TeamCollaborationEntry selectedEntry) {
        if (highlightDataSet == null || chart.getData() == null || selectedEntry == null) return;
        
        highlightDataSet.clear();
        highlightDataSet.addEntry(new Entry(selectedEntry.games, selectedEntry.success));
        
        // Match the color of the highlighted dot (green for teammate, red for opponent)
        int color = selectedEntry.isTeammate ? 
                Color.argb(80, 76, 175, 80) :   // Semi-transparent green
                Color.argb(80, 244, 67, 54);    // Semi-transparent red
        highlightDataSet.setColor(color);
        
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }
    
    private void clearHighlightRing() {
        if (highlightDataSet == null || chart.getData() == null) return;
        
        highlightedEntry = null;
        highlightDataSet.clear();
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }
    
    private void updateInfoPanel(TeamCollaborationEntry entry) {
        infoPanel.setVisibility(View.VISIBLE);
        selectedName.setText(entry.playerName);
        
        String sign = entry.success >= 0 ? "+" : "";
        String relationship = entry.isTeammate ? "With" : "Vs";
        selectedStats.setText(String.format(java.util.Locale.getDefault(),
                "%s: %d games, %s%d success",
                relationship, entry.games, sign, entry.success));
    }
    
    private void hideInfoPanel() {
        infoPanel.setVisibility(View.INVISIBLE);
    }

    private void updateChart() {
        if (allEntries == null || allEntries.isEmpty()) {
            return;
        }

        // Find max games for X-axis range
        int maxGames = 0;
        float maxAbsSuccess = 0;
        
        for (TeamCollaborationEntry entry : allEntries) {
            if (entry.games > maxGames) maxGames = entry.games;
            maxAbsSuccess = Math.max(maxAbsSuccess, Math.abs(entry.success));
        }
        
        // Include player's overall success in the range calculation
        maxAbsSuccess = Math.max(maxAbsSuccess, Math.abs(playerOverallSuccess));

        // Set axis ranges
        float xMax = maxGames + 5;
        chart.getXAxis().setAxisMaximum(xMax);
        float yRange = Math.max(maxAbsSuccess + 3, 10);
        chart.getAxisLeft().setAxisMinimum(-yRange);
        chart.getAxisLeft().setAxisMaximum(yRange);

        // Find extreme entries (top N positive and top N negative success)
        extremeIndices = new HashSet<>();
        List<Integer> sortedBySuccess = new ArrayList<>();
        for (int i = 0; i < allEntries.size(); i++) {
            sortedBySuccess.add(i);
        }
        sortedBySuccess.sort((a, b) ->
                Integer.compare(allEntries.get(b).success, allEntries.get(a).success));
        
        // Add top N (highest success)
        for (int i = 0; i < Math.min(EXTREME_LABELS_COUNT, sortedBySuccess.size()); i++) {
            extremeIndices.add(sortedBySuccess.get(i));
        }
        // Add bottom N (lowest success)
        for (int i = 0; i < Math.min(EXTREME_LABELS_COUNT, sortedBySuccess.size()); i++) {
            extremeIndices.add(sortedBySuccess.get(sortedBySuccess.size() - 1 - i));
        }

        // Create separate entry lists for each team
        ArrayList<Entry> teammateEntries = new ArrayList<>();
        ArrayList<Entry> opponentEntries = new ArrayList<>();

        for (int i = 0; i < allEntries.size(); i++) {
            TeamCollaborationEntry entry = allEntries.get(i);

            float x = entry.games;       // X: number of games
            float y = entry.success;     // Y: success rate

            Entry scatterEntry = new Entry(x, y);
            scatterEntry.setData(i); // Store index for marker lookup
            
            if (entry.isTeammate) {
                teammateEntries.add(scatterEntry);
            } else {
                opponentEntries.add(scatterEntry);
            }
        }

        ArrayList<IScatterDataSet> dataSets = new ArrayList<>();
        
        // Create highlight dataset (empty initially, will be filled when a point is selected)
        ArrayList<Entry> highlightEntries = new ArrayList<>();
        highlightDataSet = new ScatterDataSet(highlightEntries, "Highlight");
        highlightDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        highlightDataSet.setScatterShapeSize(HIGHLIGHT_SIZE);
        highlightDataSet.setColor(Color.argb(80, 100, 100, 100));
        highlightDataSet.setDrawValues(false);
        highlightDataSet.setHighlightEnabled(false);  // Don't allow selecting the highlight ring itself
        dataSets.add(highlightDataSet);
        
        // Value formatter for showing player names
        // Shows all labels when zoomed in, only extreme values when zoomed out
        ValueFormatter nameFormatter = new ValueFormatter() {
            @Override
            public String getPointLabel(Entry entry) {
                Object data = entry.getData();
                if (data != null) {
                    int index = (int) data;
                    if (index >= 0 && index < allEntries.size()) {
                        // Show all labels when zoomed in, or only extreme values when zoomed out
                        if (showingAllLabels || extremeIndices.contains(index)) {
                            return allEntries.get(index).playerName;
                        }
                    }
                }
                return "";
            }
        };

        // Create dataset for teammates - green for "with"
        if (!teammateEntries.isEmpty()) {
            teammateEntries.sort(new EntryXComparator());
            ScatterDataSet teammateDataSet = new ScatterDataSet(teammateEntries, "With");
            teammateDataSet.setColor(Color.argb(200, 76, 175, 80)); // Green
            teammateDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            teammateDataSet.setScatterShapeSize(MARKER_SIZE);
            configureDataSet(teammateDataSet, nameFormatter);
            dataSets.add(teammateDataSet);
        }
        
        // Create dataset for opponents - red for "against"
        if (!opponentEntries.isEmpty()) {
            opponentEntries.sort(new EntryXComparator());
            ScatterDataSet opponentDataSet = new ScatterDataSet(opponentEntries, "Against");
            opponentDataSet.setColor(Color.argb(200, 244, 67, 54)); // Red
            opponentDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            opponentDataSet.setScatterShapeSize(MARKER_SIZE);
            configureDataSet(opponentDataSet, nameFormatter);
            dataSets.add(opponentDataSet);
        }

        ScatterData scatterData = new ScatterData(dataSets);
        chart.setData(scatterData);
        
        // Offset to prevent label clipping at edges (left, top, right, bottom)
        chart.setExtraOffsets(10, 20, 20, 15);
        
        // Restore highlight if there was a highlighted entry before chart rebuild
        if (highlightedEntry != null) {
            updateHighlightRing(highlightedEntry);
        }
        
        chart.invalidate();
    }
    
    private void configureDataSet(ScatterDataSet dataSet, ValueFormatter formatter) {
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueFormatter(formatter);
        // Disable crosshair highlight indicators
        dataSet.setDrawHighlightIndicators(false);
    }
}
