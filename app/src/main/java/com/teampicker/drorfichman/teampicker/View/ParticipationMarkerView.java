package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.teampicker.drorfichman.teampicker.R;

import java.util.List;
import java.util.Locale;

/**
 * Marker view for participation chart.
 * Shows period, participation rate, and win/loss/tie breakdown when tapped.
 */
public class ParticipationMarkerView extends MarkerView {

    private final TextView periodView;
    private final TextView participationView;
    private final TextView resultsView;
    private final List<QuarterData> quarterDataList;
    private MPPointF offset;

    /**
     * Data class holding stats for a quarter
     */
    public static class QuarterData {
        public final String periodLabel;
        public final int playerGames;
        public final int totalGames;
        public final int wins;
        public final int ties;
        public final int losses;
        public final float participationRate;

        public QuarterData(String periodLabel, int playerGames, int totalGames, int wins, int ties, int losses) {
            this.periodLabel = periodLabel;
            this.playerGames = playerGames;
            this.totalGames = totalGames;
            this.wins = wins;
            this.ties = ties;
            this.losses = losses;
            this.participationRate = totalGames > 0 ? (playerGames * 100f / totalGames) : 0f;
        }
    }

    public ParticipationMarkerView(Context context, List<QuarterData> quarterDataList) {
        super(context, R.layout.participation_marker_view);
        this.quarterDataList = quarterDataList;

        periodView = findViewById(R.id.marker_period);
        participationView = findViewById(R.id.marker_participation);
        resultsView = findViewById(R.id.marker_results);
        offset = new MPPointF();
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();

        if (index >= 0 && index < quarterDataList.size()) {
            QuarterData data = quarterDataList.get(index);

            periodView.setText(data.periodLabel);
            participationView.setText(String.format(Locale.getDefault(),
                    "%.0f%% (%d/%d games)",
                    data.participationRate, data.playerGames, data.totalGames));

            if (data.playerGames > 0) {
                resultsView.setText(String.format(Locale.getDefault(),
                        "W: %d  T: %d  L: %d",
                        data.wins, data.ties, data.losses));
                resultsView.setVisibility(VISIBLE);
            } else {
                resultsView.setVisibility(GONE);
            }
        }

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }

    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
        Chart chart = getChartView();
        if (chart == null) {
            return getOffset();
        }

        float chartWidth = chart.getWidth();
        float markerWidth = getWidth();
        float markerHeight = getHeight();
        float padding = 20f;

        float offsetX = -(markerWidth / 2f);
        float offsetY;

        // Calculate where the marker would be if shown above
        float markerTopIfAbove = posY - markerHeight - 15;

        // If showing above would clip (marker top goes above chart), show below instead
        if (markerTopIfAbove < 0) {
            offsetY = 20; // Show below the data point
        } else {
            offsetY = -markerHeight - 15; // Show above the data point
        }

        float markerLeft = posX + offsetX;
        float markerRight = posX + offsetX + markerWidth;

        // Adjust horizontal position if marker would go off screen
        if (markerLeft < padding) {
            offsetX = padding - posX;
        } else if (markerRight > chartWidth - padding) {
            offsetX = chartWidth - padding - posX - markerWidth;
        }

        offset.x = offsetX;
        offset.y = offsetY;
        return offset;
    }
}

