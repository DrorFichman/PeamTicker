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
 * Marker view for the collaboration chart in player insights.
 * Shows player name and stats when a data point is tapped.
 */
public class CollaborationChartMarkerView extends MarkerView {

    private final TextView playerNameView;
    private final TextView statsView;
    private final List<PlayerCollaborationChartFragment.CollaborationEntry> entries;
    private MPPointF offset;

    public CollaborationChartMarkerView(Context context, 
            List<PlayerCollaborationChartFragment.CollaborationEntry> entries) {
        super(context, R.layout.team_collaboration_marker_view);
        this.entries = entries;
        
        playerNameView = findViewById(R.id.marker_player_name);
        statsView = findViewById(R.id.marker_stats);
        offset = new MPPointF();
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        Object data = e.getData();
        if (data == null) {
            super.refreshContent(e, highlight);
            return;
        }
        
        int index = (int) data;
        
        if (index >= 0 && index < entries.size()) {
            PlayerCollaborationChartFragment.CollaborationEntry entry = entries.get(index);
            
            playerNameView.setText(entry.playerName);
            
            String sign = entry.success >= 0 ? "+" : "";
            String relationship = entry.isWith ? "With" : "Vs";
            
            statsView.setText(String.format(Locale.getDefault(),
                    "%s: %d games, %s%d success",
                    relationship,
                    entry.games,
                    sign,
                    entry.success));
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
        float offsetY = -markerHeight - 10;

        float markerLeft = posX + offsetX;
        float markerRight = posX + offsetX + markerWidth;

        if (markerLeft < padding) {
            offsetX = padding - posX;
        } else if (markerRight > chartWidth - padding) {
            offsetX = chartWidth - padding - posX - markerWidth;
        }

        if (posY + offsetY < padding) {
            offsetY = 10;
        }

        offset.x = offsetX;
        offset.y = offsetY;
        return offset;
    }
}

