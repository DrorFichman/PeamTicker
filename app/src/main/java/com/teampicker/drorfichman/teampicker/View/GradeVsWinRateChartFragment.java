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
import com.teampicker.drorfichman.teampicker.tools.DbAsync;

import java.util.ArrayList;
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

    private static final float MARKER_SIZE    = 50f;
    private static final float HIGHLIGHT_SIZE = 100f;

    // Match PlayerTeamCollaborationChartFragment's color scheme
    private static final int COLOR_WIN      = Color.argb(200, 76, 175, 80);  // green  — win rate ≥ 50%
    private static final int COLOR_LOSE     = Color.argb(200, 244, 67, 54);  // red    — win rate < 50%
    private static final int COLOR_WIN_HL   = Color.argb(80,  76, 175, 80);  // semi-transparent green ring
    private static final int COLOR_LOSE_HL  = Color.argb(80,  244, 67, 54);  // semi-transparent red ring

    private ScatterChart chart;
    private TextView emptyMessage;
    private LinearLayout infoPanel;
    private TextView selectedName;
    private TextView selectedGames;
    private TextView selectedWinRate;

    /** Stores player data parallel to chart entries for tap lookup. */
    private final List<Player> chartPlayers = new ArrayList<>();
    private int selectedPlayerIndex = -1;

    /** Persistent empty dataset used as a highlight ring — updated on tap without full rebuild. */
    private ScatterDataSet highlightDataSet;

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
        android.content.Context ctx = getContext();
        if (ctx == null) return;

        DbAsync.run(
                () -> {
                    ArrayList<Player> allPlayers = DbHelper.getPlayersStatistics(ctx, -1);
                    List<Player> filtered = new ArrayList<>();
                    for (Player p : allPlayers) {
                        if (p.statistics != null && p.statistics.getWinsAndLosesCount() >= MIN_GAMES) {
                            filtered.add(p);
                        }
                    }
                    return filtered;
                },
                filtered -> {
                    if (!isAdded()) return;
                    chartPlayers.clear();
                    chartPlayers.addAll(filtered);
                    if (chartPlayers.isEmpty()) {
                        showEmpty(getString(R.string.grade_winrate_no_data, MIN_GAMES));
                    } else {
                        showChart();
                    }
                });
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
        rebuildDatasets();
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

        LimitLine avgLine = new LimitLine(50f, "50%");
        avgLine.setLineColor(Color.parseColor("#9C27B0"));
        avgLine.setLineWidth(1.5f);
        avgLine.enableDashedLine(10f, 5f, 0f);
        avgLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        avgLine.setTextSize(10f);
        avgLine.setTextColor(Color.parseColor("#9C27B0"));
        chart.getAxisLeft().addLimitLine(avgLine);

        chart.setExtraOffsets(10, 20, 20, 10);

        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                Object data = e.getData();
                if (data instanceof Integer) {
                    int idx = (Integer) data;
                    if (idx >= 0 && idx < chartPlayers.size()) {
                        if (selectedPlayerIndex == idx) {
                            clearHighlight();
                        } else {
                            selectedPlayerIndex = idx;
                            showInfoPanel(chartPlayers.get(idx));
                            updateHighlightRing(chartPlayers.get(idx), e.getX(), e.getY());
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected() {
                clearHighlight();
            }
        });
    }

    /**
     * Builds the initial chart datasets (green/red dots + empty highlight ring).
     * Called once when data is ready; tap events update only the highlight ring.
     */
    private void rebuildDatasets() {
        ArrayList<Entry> winEntries  = new ArrayList<>();
        ArrayList<Entry> loseEntries = new ArrayList<>();

        for (int i = 0; i < chartPlayers.size(); i++) {
            Player p = chartPlayers.get(i);
            Entry entry = new Entry(p.statistics.getWinsAndLosesCount(), p.statistics.getWinRate());
            entry.setData(i);
            if (p.statistics.getWinRate() >= 50) {
                winEntries.add(entry);
            } else {
                loseEntries.add(entry);
            }
        }

        winEntries.sort(new EntryXComparator());
        loseEntries.sort(new EntryXComparator());

        ValueFormatter nameFormatter = new ValueFormatter() {
            @Override
            public String getPointLabel(Entry entry) {
                Object d = entry.getData();
                if (d instanceof Integer) {
                    int idx = (Integer) d;
                    if (idx >= 0 && idx < chartPlayers.size()) {
                        return chartPlayers.get(idx).mName;
                    }
                }
                return "";
            }
        };

        ScatterDataSet winSet = makeDotsDataSet(winEntries, COLOR_WIN, nameFormatter);
        ScatterDataSet loseSet = makeDotsDataSet(loseEntries, COLOR_LOSE, nameFormatter);

        // Empty highlight ring — populated by updateHighlightRing() on tap
        highlightDataSet = new ScatterDataSet(new ArrayList<>(), "");
        highlightDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        highlightDataSet.setScatterShapeSize(HIGHLIGHT_SIZE);
        highlightDataSet.setColor(COLOR_WIN_HL);
        highlightDataSet.setDrawValues(false);
        highlightDataSet.setDrawHighlightIndicators(false);
        highlightDataSet.setHighlightEnabled(false); // don't let the ring itself be "selected"

        chart.setData(new ScatterData(highlightDataSet, winSet, loseSet));
        chart.invalidate();
    }

    private ScatterDataSet makeDotsDataSet(ArrayList<Entry> entries, int color, ValueFormatter formatter) {
        ScatterDataSet set = new ScatterDataSet(entries, "");
        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setScatterShapeSize(MARKER_SIZE);
        set.setColor(color);
        set.setDrawHighlightIndicators(false);
        set.setDrawValues(true);
        set.setValueTextSize(10f);
        set.setValueTextColor(Color.DKGRAY);
        set.setValueFormatter(formatter);
        return set;
    }

    /** Places the highlight ring at the tapped player's position with a matching hue. */
    private void updateHighlightRing(Player p, float x, float y) {
        if (highlightDataSet == null || chart.getData() == null) return;
        highlightDataSet.clear();
        highlightDataSet.addEntry(new Entry(x, y));
        highlightDataSet.setColor(p.statistics.getWinRate() >= 50 ? COLOR_WIN_HL : COLOR_LOSE_HL);
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    /** Clears the highlight ring and hides the info panel. */
    private void clearHighlight() {
        selectedPlayerIndex = -1;
        infoPanel.setVisibility(View.INVISIBLE);
        chart.highlightValue(null);
        if (highlightDataSet != null && chart.getData() != null) {
            highlightDataSet.clear();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    }

    private void showInfoPanel(Player p) {
        infoPanel.setVisibility(View.VISIBLE);
        selectedName.setText(p.mName);
        selectedGames.setText(String.format(Locale.getDefault(), "%dg", p.statistics.getWinsAndLosesCount()));
        selectedWinRate.setText(String.format(Locale.getDefault(), "%d%%", p.statistics.getWinRate()));
    }

    private void openSelectedPlayerDetails() {
        if (selectedPlayerIndex < 0 || selectedPlayerIndex >= chartPlayers.size() || getContext() == null) return;
        Intent intent = PlayerDetailsActivity.getPlayerWinRateIntent(getContext(), chartPlayers.get(selectedPlayerIndex).mName);
        startActivity(intent);
    }
}
