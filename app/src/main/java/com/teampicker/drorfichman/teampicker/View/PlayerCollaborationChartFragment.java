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

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
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
import java.util.Locale;

/**
 * Scatter chart showing player collaboration statistics.
 * X-axis: Games count (negative = games against, positive = games with)
 * Y-axis: Success rate (wins - losses)
 * Each point represents another player with whom the current player has played.
 */
public class PlayerCollaborationChartFragment extends Fragment {

    private static final String ARG_PLAYER = "player";
    private static final int MIN_GAMES_THRESHOLD = 5; // Minimum games with/against to show a player
    private static final int PLAYERS_COUNT = 15;

    private Player player;
    private ScatterChart chart;
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

        // Filter to players with enough games
        collaboratorsList = new ArrayList<>();
        for (PlayerChemistry pc : collaborations.values()) {
            if (pc.statisticsWith.gamesCount >= MIN_GAMES_THRESHOLD || 
                pc.statisticsVs.gamesCount >= MIN_GAMES_THRESHOLD) {
                collaboratorsList.add(pc);
            }
        }

        // Sort by max absolute success value and keep only top 15
        collaboratorsList.sort((a, b) -> {
            int absA = Math.max(Math.abs(a.statisticsWith.successRate), Math.abs(a.statisticsVs.successRate));
            int absB = Math.max(Math.abs(b.statisticsWith.successRate), Math.abs(b.statisticsVs.successRate));
            return Integer.compare(absB, absA); // Descending order
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

        // Configure X-axis (games count)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setTextSize(10f);
        xAxis.setDrawAxisLine(true);
        
        // Add vertical line at 0 (separating "with" and "against")
        LimitLine centerLine = new LimitLine(0f);
        centerLine.setLineColor(Color.DKGRAY);
        centerLine.setLineWidth(2f);
        xAxis.addLimitLine(centerLine);

        // Configure Y-axis (success = wins - losses)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setTextSize(10f);
        leftAxis.setDrawAxisLine(true);
        
        // Add horizontal line at 0
        LimitLine zeroLine = new LimitLine(0f);
        zeroLine.setLineColor(Color.DKGRAY);
        zeroLine.setLineWidth(1f);
        leftAxis.addLimitLine(zeroLine);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // Configure legend
        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextSize(11f);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);

        // Set marker view for detailed info on tap
        CollaborationMarkerView markerView = new CollaborationMarkerView(requireContext(), collaboratorsList);
        chart.setMarker(markerView);
    }

    private void updateChart() {
        if (collaboratorsList == null || collaboratorsList.isEmpty()) {
            return;
        }

        List<ScatterDataSet> dataSets = new ArrayList<>();

        // Create entries for "games with" (positive X)
        ArrayList<Entry> gamesWithEntries = new ArrayList<>();
        // Create entries for "games against" (negative X)
        ArrayList<Entry> gamesAgainstEntries = new ArrayList<>();

        for (int i = 0; i < collaboratorsList.size(); i++) {
            PlayerChemistry pc = collaboratorsList.get(i);

            // Games with - positive X axis
            if (pc.statisticsWith.gamesCount >= MIN_GAMES_THRESHOLD) {
                float x = pc.statisticsWith.gamesCount;
                float y = pc.statisticsWith.successRate; // wins - losses
                Entry entry = new Entry(x, y);
                entry.setData(i); // Store index for marker lookup
                gamesWithEntries.add(entry);
            }

            // Games against - negative X axis
            if (pc.statisticsVs.gamesCount >= MIN_GAMES_THRESHOLD) {
                float x = -pc.statisticsVs.gamesCount; // Negative for "against"
                float y = pc.statisticsVs.successRate; // wins - losses
                Entry entry = new Entry(x, y);
                entry.setData(i); // Store index for marker lookup
                gamesAgainstEntries.add(entry);
            }
        }

        // Sort entries by X value (required by MPAndroidChart)
        Collections.sort(gamesWithEntries, new EntryXComparator());
        Collections.sort(gamesAgainstEntries, new EntryXComparator());

        // Create dataset for "games with"
        if (!gamesWithEntries.isEmpty()) {
            ScatterDataSet withDataSet = new ScatterDataSet(gamesWithEntries, getString(R.string.insights_games_with_label));
            withDataSet.setColor(Color.rgb(76, 175, 80)); // Green
            withDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            withDataSet.setScatterShapeSize(24f);
            withDataSet.setDrawValues(true);
            withDataSet.setValueTextSize(8f);
            withDataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getPointLabel(Entry entry) {
                    int index = (int) entry.getData();
                    if (index >= 0 && index < collaboratorsList.size()) {
                        return collaboratorsList.get(index).mName;
                    }
                    return "";
                }
            });
            dataSets.add(withDataSet);
        }

        // Create dataset for "games against"
        if (!gamesAgainstEntries.isEmpty()) {
            ScatterDataSet againstDataSet = new ScatterDataSet(gamesAgainstEntries, getString(R.string.insights_games_against_label));
            againstDataSet.setColor(Color.rgb(244, 67, 54)); // Red
            againstDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            againstDataSet.setScatterShapeSize(24f);
            againstDataSet.setDrawValues(true);
            againstDataSet.setValueTextSize(8f);
            againstDataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getPointLabel(Entry entry) {
                    int index = (int) entry.getData();
                    if (index >= 0 && index < collaboratorsList.size()) {
                        return collaboratorsList.get(index).mName;
                    }
                    return "";
                }
            });
            dataSets.add(againstDataSet);
        }

        if (dataSets.isEmpty()) {
            showEmptyState();
            return;
        }

        ScatterData scatterData = new ScatterData();
        for (ScatterDataSet ds : dataSets) {
            scatterData.addDataSet(ds);
        }
        
        chart.setData(scatterData);
        chart.invalidate();
    }
}

