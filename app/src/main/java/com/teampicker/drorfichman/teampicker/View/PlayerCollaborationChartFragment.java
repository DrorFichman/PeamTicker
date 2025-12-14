package com.teampicker.drorfichman.teampicker.View;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.github.mikephil.charting.utils.EntryXComparator;
import com.teampicker.drorfichman.teampicker.Data.BuilderPlayerCollaborationStatistics;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerChemistry;
import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Scatter chart showing player collaboration statistics.
 * X-axis: Number of games
 * Y-axis: Success rate (wins - losses)
 * Color: Green for "with" success, Red/Orange for "against" success
 * Shows top 30 players by absolute success value.
 */
public class PlayerCollaborationChartFragment extends Fragment {

    private static final String ARG_PLAYER = "player";
    private static final int MIN_GAMES_THRESHOLD = 3;
    private static final int PLAYERS_COUNT = 40;
    private static final float MARKER_SIZE = 50f;  // Fixed pixel size for all markers
    private static final float HIGHLIGHT_SIZE = 80f;  // Size of highlight ring around selected dot
    private static final int EXTREME_LABELS_COUNT = 5;  // Show labels only for top/bottom N players
    private static final float SHOW_ALL_LABELS_THRESHOLD = 40f;  // Show all labels when X range is this or smaller

    private Player player;
    private ScatterChart chart;
    private TextView emptyMessage;
    private LinearLayout infoPanel;
    private TextView selectedName;
    private TextView selectedStats;
    
    // Player's overall success rate for the horizontal reference line
    private int playerOverallSuccess = 0;
    
    // Data for marker view and chart
    private List<CollaborationEntry> allEntries;
    
    // Indices of entries that should show labels (extreme values)
    private java.util.Set<Integer> extremeIndices;
    
    // Highlight dataset for showing selection ring
    private ScatterDataSet highlightDataSet;
    
    // Track whether all labels are currently shown
    private boolean showingAllLabels = false;

    /**
     * Data class to hold collaboration entry info
     */
    static class CollaborationEntry {
        String playerName;
        int games;
        int success;
        boolean isWith; // true = "with" stats, false = "against" stats
        
        CollaborationEntry(String name, int games, int success, boolean isWith) {
            this.playerName = name;
            this.games = games;
            this.success = success;
            this.isWith = isWith;
        }
    }

    public PlayerCollaborationChartFragment() {
        super(R.layout.fragment_player_collaboration_chart);
    }

    public static PlayerCollaborationChartFragment newInstance(Player player) {
        PlayerCollaborationChartFragment fragment = new PlayerCollaborationChartFragment();
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
        chart = root.findViewById(R.id.collaboration_chart);
        emptyMessage = root.findViewById(R.id.collaboration_empty_message);
        TextView chartTitle = root.findViewById(R.id.collaboration_chart_title);
        infoPanel = root.findViewById(R.id.collaboration_info_panel);
        selectedName = root.findViewById(R.id.collaboration_selected_name);
        selectedStats = root.findViewById(R.id.collaboration_selected_stats);

        loadDataAndSetupChart();

        return root;
    }

    private void loadDataAndSetupChart() {
        if (player == null || getContext() == null) {
            showEmptyState();
            return;
        }

        // Load player with statistics to get overall success rate
        Player playerWithStats = DbHelper.getPlayer(getContext(), player.mName, -1);
        if (playerWithStats != null && playerWithStats.statistics != null) {
            playerOverallSuccess = playerWithStats.statistics.successRate;
        }

        // Get collaboration statistics for all games
        HashMap<String, PlayerChemistry> collaborations = DbHelper.getPlayersParticipationStatistics(
                getContext(), player.mName,
                new BuilderPlayerCollaborationStatistics().setGames(-1));

        if (collaborations == null || collaborations.isEmpty()) {
            showEmptyState();
            return;
        }

        // Build entries list - include both "with" and "against" stats
        List<CollaborationEntry> tempEntries = new ArrayList<>();
        
        for (PlayerChemistry pc : collaborations.values()) {
            // Add "with" entry if enough games
            if (pc.statisticsWith.gamesCount >= MIN_GAMES_THRESHOLD) {
                tempEntries.add(new CollaborationEntry(
                        pc.mName,
                        pc.statisticsWith.gamesCount,
                        pc.statisticsWith.successRate,
                        true));
            }
            
            // Add "against" entry if enough games
            if (pc.statisticsVs.gamesCount >= MIN_GAMES_THRESHOLD) {
                tempEntries.add(new CollaborationEntry(
                        pc.mName,
                        pc.statisticsVs.gamesCount,
                        pc.statisticsVs.successRate,
                        false));
            }
        }

        // Sort by absolute success value (descending) and keep top players
        tempEntries.sort((a, b) -> Integer.compare(Math.abs(b.success), Math.abs(a.success)));

        if (tempEntries.size() > PLAYERS_COUNT) {
            allEntries = new ArrayList<>(tempEntries.subList(0, PLAYERS_COUNT));
        } else {
            allEntries = tempEntries;
        }

        if (allEntries.isEmpty()) {
            showEmptyState();
            return;
        }

        setupChart();
        updateChart();
    }

    private void showEmptyState() {
        chart.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(getString(R.string.insights_collaboration_no_data, MIN_GAMES_THRESHOLD));
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
        
        // X-axis label via chart description
        chart.getDescription().setEnabled(true);
        chart.getDescription().setText(getString(R.string.team_collaboration_x_axis_label));
        chart.getDescription().setTextSize(11f);
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
        LimitLine playerSuccessLine = new LimitLine(playerOverallSuccess, player.mName + " overall success");
        playerSuccessLine.setLineColor(Color.parseColor("#9C27B0")); // Purple
        playerSuccessLine.setLineWidth(2f);
        playerSuccessLine.enableDashedLine(10f, 5f, 0f);
        playerSuccessLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        playerSuccessLine.setTextSize(10f);
        playerSuccessLine.setTextColor(Color.parseColor("#9C27B0"));
        leftAxis.addLimitLine(playerSuccessLine);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // Hide legend - colors explained in title
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
                        CollaborationEntry entry = allEntries.get(index);
                        updateInfoPanel(entry);
                        updateHighlightRing(e.getX(), e.getY(), entry.isWith);
                    }
                }
            }

            @Override
            public void onNothingSelected() {
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
    
    private void updateHighlightRing(float x, float y, boolean isWith) {
        if (highlightDataSet == null || chart.getData() == null) return;
        
        highlightDataSet.clear();
        highlightDataSet.addEntry(new Entry(x, y));
        
        // Match the color of the highlighted dot (green for "with", red for "against")
        int color = isWith ? 
                Color.argb(80, 76, 175, 80) :   // Semi-transparent green
                Color.argb(80, 244, 67, 54);    // Semi-transparent red
        highlightDataSet.setColor(color);
        
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }
    
    private void clearHighlightRing() {
        if (highlightDataSet == null || chart.getData() == null) return;
        
        highlightDataSet.clear();
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }
    
    private void updateInfoPanel(CollaborationEntry entry) {
        infoPanel.setVisibility(View.VISIBLE);
        selectedName.setText(entry.playerName);
        
        String sign = entry.success >= 0 ? "+" : "";
        String relationship = entry.isWith ? "With" : "Vs";
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
        
        for (CollaborationEntry entry : allEntries) {
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
        extremeIndices = new java.util.HashSet<>();
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

        // Create separate entry lists for "with" and "against"
        ArrayList<Entry> withEntries = new ArrayList<>();
        ArrayList<Entry> againstEntries = new ArrayList<>();

        for (int i = 0; i < allEntries.size(); i++) {
            CollaborationEntry entry = allEntries.get(i);

            float x = entry.games;       // X: number of games
            float y = entry.success;     // Y: success rate

            Entry scatterEntry = new Entry(x, y);
            scatterEntry.setData(i); // Store index for marker lookup
            
            if (entry.isWith) {
                withEntries.add(scatterEntry);
            } else {
                againstEntries.add(scatterEntry);
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

        // Green for "with" success
        if (!withEntries.isEmpty()) {
            withEntries.sort(new EntryXComparator());
            ScatterDataSet withDataSet = new ScatterDataSet(withEntries, "With");
            withDataSet.setColor(Color.argb(200, 76, 175, 80)); // Green
            withDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            withDataSet.setScatterShapeSize(MARKER_SIZE);
            configureDataSet(withDataSet, nameFormatter);
            dataSets.add(withDataSet);
        }
        
        // Orange/Red for "against" success
        if (!againstEntries.isEmpty()) {
            againstEntries.sort(new EntryXComparator());
            ScatterDataSet againstDataSet = new ScatterDataSet(againstEntries, "Against");
            againstDataSet.setColor(Color.argb(200, 244, 67, 54)); // Red
            againstDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            againstDataSet.setScatterShapeSize(MARKER_SIZE);
            configureDataSet(againstDataSet, nameFormatter);
            dataSets.add(againstDataSet);
        }

        ScatterData scatterData = new ScatterData(dataSets);
        chart.setData(scatterData);
        
        // Offset to prevent label clipping at edges
        chart.setExtraOffsets(10, 20, 20, 15);
        
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
