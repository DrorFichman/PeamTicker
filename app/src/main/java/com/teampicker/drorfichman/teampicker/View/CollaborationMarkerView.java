package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.teampicker.drorfichman.teampicker.Data.PlayerChemistry;
import com.teampicker.drorfichman.teampicker.R;

import java.util.List;
import java.util.Locale;

/**
 * Marker view for the collaboration scatter chart.
 * Shows player name and detailed stats when a data point is tapped.
 */
public class CollaborationMarkerView extends MarkerView {

    private final TextView playerNameView;
    private final TextView gamesWithView;
    private final TextView gamesAgainstView;
    private final List<PlayerChemistry> collaborators;
    private MPPointF offset;

    public CollaborationMarkerView(Context context, List<PlayerChemistry> collaborators) {
        super(context, R.layout.collaboration_marker_view);
        this.collaborators = collaborators;
        
        playerNameView = findViewById(R.id.marker_player_name);
        gamesWithView = findViewById(R.id.marker_games_with);
        gamesAgainstView = findViewById(R.id.marker_games_against);
        offset = new MPPointF();
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        // Entry data contains the index in our collaborators list
        int index = (int) e.getData();
        
        if (index >= 0 && index < collaborators.size()) {
            PlayerChemistry player = collaborators.get(index);
            
            playerNameView.setText(player.mName);
            
            // Games with stats
            if (player.statisticsWith.gamesCount > 0) {
                gamesWithView.setText(String.format(Locale.getDefault(),
                        "With: %d games, %d%% win rate",
                        player.statisticsWith.gamesCount,
                        player.statisticsWith.getWinRate()));
                gamesWithView.setVisibility(VISIBLE);
            } else {
                gamesWithView.setVisibility(GONE);
            }
            
            // Games against stats
            if (player.statisticsVs.gamesCount > 0) {
                gamesAgainstView.setText(String.format(Locale.getDefault(),
                        "Vs: %d games, %d%% win rate",
                        player.statisticsVs.gamesCount,
                        player.statisticsVs.getWinRate()));
                gamesAgainstView.setVisibility(VISIBLE);
            } else {
                gamesAgainstView.setVisibility(GONE);
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
        float chartHeight = chart.getHeight();
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

