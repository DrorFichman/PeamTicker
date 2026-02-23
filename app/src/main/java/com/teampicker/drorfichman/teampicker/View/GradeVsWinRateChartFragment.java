package com.teampicker.drorfichman.teampicker.View;

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
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Scatter chart: X = games played, Y = win rate %.
 * Top-right = reliable winners; top-left = high win rate but few games (possible noise).
 * A horizontal dashed line marks the group average win rate.
 * Tap a dot to navigate to the player's details.
 */
public class GradeVsWinRateChartFragment extends Fragment {

    private static final int MIN_GAMES = 5;

    private ScatterChart chart;
    private TextView emptyMessage;
    private LinearLayout infoPanel;
    private TextView selectedName;
    private TextView selectedGames;
    private TextView selectedWinRate;

    /** Stores player data parallel to chart entries for tap lookup. */
    private final List<Player> chartPlayers = new ArrayList<>();
    private int selectedPlayerIndex = -1;

    public GradeVsWinRateChartFragment() {
        super(R.layout.fragment_grade_vs_winrate_chart);
    }

    public static GradeVsWinRateChartFragment newInstance() {
        return new GradeVsWinRateChartFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        assert root != null;

        chart = root.findViewById(R.id.grade_winrate_chart);
        emptyMessage = root.findViewById(R.id.grade_winrate_empty);
        infoPanel = root.findViewById(R.id.grade_winrate_info_panel);
        selectedName = root.findViewById(R.id.grade_winrate_selected_name);
        selectedGames = root.findViewById(R.id.grade_winrate_selected_games);
        selectedWinRate = root.findViewById(R.id.grade_winrate_selected_winrate);

        infoPanel.setOnClickListener(v -> openSelectedPlayerDetails());

        loadData();
        return root;
    }

    private void loadData() {
        if (getContext() == null) return;

        ArrayList<Player> allPlayers = DbHelper.getPlayersStatistics(getContext(), -1);

        chartPlayers.clear();
        for (Player p : allPlayers) {
            if (p.statistics != null && p.statistics.getWinsAndLosesCount() >= MIN_GAMES) {
                chartPlayers.add(p);
            }
        }

        if (chartPlayers.isEmpty()) {
            showEmpty(getString(R.string.grade_winrate_no_data, MIN_GAMES));
            return;
        }

        showChart();
    }

    private void showEmpty(String message) {
        chart.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(message);
    }

    private void showChart() {
        emptyMessage.setVisibility(View.GONE);
        chart.setVisibility(View.VISIBLE);
        setupChart();
        populateChart();
    }

    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(Color.WHITE);
        chart.getLegend().setEnabled(false);

        // X-axis: games played
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setTextSize(10f);
        xAxis.setGranularity(5f);
        xAxis.setAxisMinimum(0f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + " games";
            }
        });

        // Y-axis: win rate %
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setTextSize(10f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f%%", value);
            }
        });

        chart.getAxisRight().setEnabled(false);

        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                Object data = e.getData();
                if (data instanceof Integer) {
                    int idx = (Integer) data;
                    if (idx >= 0 && idx < chartPlayers.size()) {
                        selectedPlayerIndex = idx;
                        showInfoPanel(chartPlayers.get(idx));
                    }
                }
            }

            @Override
            public void onNothingSelected() {
                selectedPlayerIndex = -1;
                infoPanel.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void populateChart() {
        ArrayList<Entry> playerEntries = new ArrayList<>();
        for (int i = 0; i < chartPlayers.size(); i++) {
            Player p = chartPlayers.get(i);
            float gamesPlayed = p.statistics.getWinsAndLosesCount();
            float winRate = p.statistics.getWinRate();
            Entry entry = new Entry(gamesPlayed, winRate);
            entry.setData(Integer.valueOf(i));
            playerEntries.add(entry);
        }

        playerEntries.sort(new EntryXComparator());

        ScatterDataSet playerSet = new ScatterDataSet(playerEntries, "Players");
        playerSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        playerSet.setScatterShapeSize(40f);
        playerSet.setColor(Color.argb(200, 33, 150, 243)); // blue
        playerSet.setDrawValues(true);
        playerSet.setValueTextSize(9f);
        playerSet.setValueTextColor(Color.DKGRAY);
        playerSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPointLabel(Entry entry) {
                Object data = entry.getData();
                if (data instanceof Integer) {
                    int idx = (Integer) data;
                    if (idx >= 0 && idx < chartPlayers.size()) {
                        return chartPlayers.get(idx).mName;
                    }
                }
                return "";
            }
        });
        playerSet.setDrawHighlightIndicators(false);

        ScatterData data = new ScatterData(playerSet);
        chart.setData(data);

        LimitLine avgLine = new LimitLine(50f, "50%");
        avgLine.setLineColor(Color.parseColor("#9C27B0")); // purple
        avgLine.setLineWidth(1.5f);
        avgLine.enableDashedLine(10f, 5f, 0f);
        avgLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        avgLine.setTextSize(10f);
        avgLine.setTextColor(Color.parseColor("#9C27B0"));
        chart.getAxisLeft().addLimitLine(avgLine);

        chart.setExtraOffsets(10, 20, 20, 10);
        chart.invalidate();
    }

    private void showInfoPanel(Player p) {
        infoPanel.setVisibility(View.VISIBLE);
        selectedName.setText(p.mName);
        selectedGames.setText(String.format(Locale.getDefault(), "%d games", p.statistics.getWinsAndLosesCount()));
        selectedWinRate.setText(String.format(Locale.getDefault(), "%d%% WR", p.statistics.getWinRate()));
    }

    private void openSelectedPlayerDetails() {
        if (selectedPlayerIndex < 0 || selectedPlayerIndex >= chartPlayers.size() || getContext() == null) return;
        Intent intent = PlayerDetailsActivity.getEditPlayerIntent(getContext(), chartPlayers.get(selectedPlayerIndex).mName);
        startActivity(intent);
    }
}
