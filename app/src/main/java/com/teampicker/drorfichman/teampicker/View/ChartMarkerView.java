package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.teampicker.drorfichman.teampicker.Data.PlayerGameStat;
import com.teampicker.drorfichman.teampicker.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChartMarkerView extends MarkerView {

    private final TextView dateTextView;
    private final TextView valueTextView;
    private final ArrayList<PlayerGameStat> gameHistory;
    private final SimpleDateFormat dateFormat;
    private MPPointF offset;

    public ChartMarkerView(Context context, ArrayList<PlayerGameStat> gameHistory) {
        super(context, R.layout.chart_marker_view);
        this.gameHistory = gameHistory;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        
        dateTextView = findViewById(R.id.marker_date);
        valueTextView = findViewById(R.id.marker_value);
        offset = new MPPointF();
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        
        // Get date from game history
        if (index >= 0 && index < gameHistory.size()) {
            Date gameDate = gameHistory.get(index).getDate();
            if (gameDate != null) {
                dateTextView.setText(dateFormat.format(gameDate));
            } else {
                dateTextView.setText("Game " + (index + 1));
            }
        } else {
            dateTextView.setText("Game " + (index + 1));
        }
        
        // Format win rate
        float winRate = e.getY();
        valueTextView.setText(String.format(Locale.getDefault(), "Win Rate: %.1f%%", winRate));
        
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // Default offset - will be adjusted in getOffsetForDrawingAtPoint
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
        float padding = 20f; // Extra padding from edges

        // Default: center above the point
        float offsetX = -(markerWidth / 2f);
        float offsetY = -markerHeight - 10; // 10px padding above the point

        // Calculate where marker would be drawn
        float markerLeft = posX + offsetX;
        float markerRight = posX + offsetX + markerWidth;

        // Adjust horizontal position if marker would go off screen
        if (markerLeft < padding) {
            // Too far left - position marker to the right of touch point
            offsetX = padding - posX;
        } else if (markerRight > chartWidth - padding) {
            // Too far right - position marker to the left of touch point
            offsetX = chartWidth - padding - posX - markerWidth;
        }

        // Adjust vertical position if marker would go off top
        if (posY + offsetY < padding) {
            // Not enough space above - show below the point
            offsetY = 10;
        }

        offset.x = offsetX;
        offset.y = offsetY;
        return offset;
    }
}

