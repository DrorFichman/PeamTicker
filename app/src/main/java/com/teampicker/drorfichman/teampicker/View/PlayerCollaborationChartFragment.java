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

import com.github.mikephil.charting.charts.BubbleChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BubbleData;
import com.github.mikephil.charting.data.BubbleDataSet;
import com.github.mikephil.charting.data.BubbleEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.teampicker.drorfichman.teampicker.Data.BuilderPlayerCollaborationStatistics;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerChemistry;
import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Bubble chart showing player collaboration statistics.
 * X-axis: Success with (wins - losses when playing together)
 * Y-axis: Success against (wins - losses when playing against)
 * Bubble size: Total games (with + against)
 * Each bubble represents another player.
 */
public class PlayerCollaborationChartFragment extends Fragment {

    private static final String ARG_PLAYER = "player";
    private static final int MIN_GAMES_THRESHOLD = 5; // Minimum total games to show a player
    private static final int PLAYERS_COUNT = 20;

    private Player player;
    private BubbleChart chart;
    private TextView emptyMessage;
    private TextView chartTitle;
    private List<PlayerChemistry> collaboratorsList;

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

        chart = root.findViewById(R.id.collaboration_chart);
        emptyMessage = root.findViewById(R.id.collaboration_empty_message);
        chartTitle = root.findViewById(R.id.collaboration_chart_title);

        loadDataAndSetupChart();

        return root;
    }

    private void loadDataAndSetupChart() {
        if (player == null || getContext() == null) {
            showEmptyState();
            return;
        }

        // Get collaboration statistics for all games
        HashMap<String, PlayerChemistry> collaborations = DbHelper.getPlayersParticipationStatistics(
                getContext(), player.mName,
                new BuilderPlayerCollaborationStatistics().setGames(-1));

        if (collaborations == null || collaborations.isEmpty()) {
            showEmptyState();
            return;
        }

        // Filter to players with enough games (both with AND against required)
        collaboratorsList = new ArrayList<>();
        for (PlayerChemistry pc : collaborations.values()) {
            int totalGames = pc.statisticsWith.gamesCount + pc.statisticsVs.gamesCount;
            if (totalGames >= MIN_GAMES_THRESHOLD && 
                pc.statisticsWith.gamesCount > 0 && 
                pc.statisticsVs.gamesCount > 0) {
                collaboratorsList.add(pc);
            }
        }

        // Sort by total games and keep top players
        collaboratorsList.sort((a, b) -> {
            int totalA = a.statisticsWith.gamesCount + a.statisticsVs.gamesCount;
            int totalB = b.statisticsWith.gamesCount + b.statisticsVs.gamesCount;
            return Integer.compare(totalB, totalA); // Descending order
        });

        if (collaboratorsList.size() > PLAYERS_COUNT) {
            collaboratorsList = new ArrayList<>(collaboratorsList.subList(0, PLAYERS_COUNT));
        }

        if (collaboratorsList.isEmpty()) {
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
        chart.setExtraTopOffset(70f);
        chart.setExtraRightOffset(30f);
        chart.setExtraBottomOffset(15f);

        // Configure X-axis (Success With)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setTextSize(10f);
        xAxis.setDrawAxisLine(true);

        // Add vertical line at 0 with label
        LimitLine xZeroLine = new LimitLine(0f);
        xZeroLine.setLineColor(Color.DKGRAY);
        xZeroLine.setLineWidth(1f);
        xAxis.addLimitLine(xZeroLine);

        // Configure Y-axis (Success Against)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setTextSize(10f);
        leftAxis.setDrawAxisLine(true);

        // Add horizontal line at 0
        LimitLine yZeroLine = new LimitLine(0f);
        yZeroLine.setLineColor(Color.DKGRAY);
        yZeroLine.setLineWidth(1f);
        leftAxis.addLimitLine(yZeroLine);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // Configure legend
        chart.getLegend().setEnabled(false); // Single dataset, no need for legend

        // Set marker view for detailed info on tap
        CollaborationMarkerView markerView = new CollaborationMarkerView(requireContext(), collaboratorsList);
        chart.setMarker(markerView);
    }

    private void updateChart() {
        if (collaboratorsList == null || collaboratorsList.isEmpty()) {
            return;
        }

        ArrayList<BubbleEntry> entries = new ArrayList<>();

        // Find max total games and max absolute values for scaling
        int maxTotalGames = 0;
        int minTotalGames = Integer.MAX_VALUE;
        float maxAbsValue = 0;
        
        for (PlayerChemistry pc : collaboratorsList) {
            int total = pc.statisticsWith.gamesCount + pc.statisticsVs.gamesCount;
            if (total > maxTotalGames) maxTotalGames = total;
            if (total < minTotalGames) minTotalGames = total;
            
            // Track max absolute value for centering the chart
            maxAbsValue = Math.max(maxAbsValue, Math.abs(pc.statisticsWith.successRate));
            maxAbsValue = Math.max(maxAbsValue, Math.abs(pc.statisticsVs.successRate));
        }

        // Set symmetric axis ranges to center the chart around 0
        float axisRange = Math.max(maxAbsValue + 5, 10); // At least 10, with some padding
        chart.getXAxis().setAxisMinimum(-axisRange);
        chart.getXAxis().setAxisMaximum(axisRange);
        chart.getAxisLeft().setAxisMinimum(-axisRange);
        chart.getAxisLeft().setAxisMaximum(axisRange);

        for (int i = 0; i < collaboratorsList.size(); i++) {
            PlayerChemistry pc = collaboratorsList.get(i);

            float x = pc.statisticsWith.successRate;  // Success with
            float y = pc.statisticsVs.successRate;    // Success against
            int totalGames = pc.statisticsWith.gamesCount + pc.statisticsVs.gamesCount;
            
            // Scale bubble size with much wider range for visible differences
            // Use sqrt scaling for better visual representation
            float normalizedGames = (float)(totalGames - minTotalGames) / Math.max(maxTotalGames - minTotalGames, 1);
            float bubbleSize = 2f + (4f * (float)Math.sqrt(normalizedGames));

            BubbleEntry entry = new BubbleEntry(x, y, bubbleSize);
            entry.setData(i); // Store index for marker lookup
            entries.add(entry);
        }

        Collections.sort(entries, new EntryXComparator());

        BubbleDataSet dataSet = new BubbleDataSet(entries, getString(R.string.insights_collaboration_title));
        dataSet.setColor(Color.argb(180, 33, 150, 243)); // Semi-transparent blue
        dataSet.setHighlightCircleWidth(1.5f);
        dataSet.setNormalizeSizeEnabled(false); // Use actual size values
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(8f);
        dataSet.setValueTextColor(Color.DKGRAY);
        
        // Custom formatter to show name with Y offset effect by adding newline prefix
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBubbleLabel(BubbleEntry entry) {
                Object data = entry.getData();
                if (data != null) {
                    int index = (int) data;
                    if (index >= 0 && index < collaboratorsList.size()) {
                        // Add newlines before name to push label above the bubble
                        return collaboratorsList.get(index).mName;
                    }
                }
                return "";
            }
        });

        BubbleData bubbleData = new BubbleData(dataSet);
        chart.setData(bubbleData);
        
        // Offset to prevent label clipping at edges
        chart.setExtraOffsets(10, 70, 30, 15);
        
        // Center the Y-axis description label at the top after layout
        chart.post(() -> {
            float centerX = chart.getWidth() / 2f;
            chart.getDescription().setPosition(centerX, 20f);
            chart.invalidate();
        });
        
        chart.invalidate();
    }
}
