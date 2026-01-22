package com.teampicker.drorfichman.teampicker.Controller.TeamDivision;

import android.content.Context;

import com.teampicker.drorfichman.teampicker.Data.BuilderPlayerCollaborationStatistics;
import com.teampicker.drorfichman.teampicker.Data.Player;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class DivideCollaboration extends DividerBase {

    BuilderPlayerCollaborationStatistics params;

    @Override
    int gradeOption(Context ctx, OptionalDivision option) {
        return option.getBalanceScore(ctx, params);
    }

    @Override
    public void divide(Context ctx, @NonNull ArrayList<Player> comingPlayers,
                       @NonNull List<Player> players1,
                       @NonNull List<Player> players2, int divideAttemptsCount, TeamDivision.onTaskInProgress update) {
        params = new BuilderPlayerCollaborationStatistics().setCached();
        super.divide(ctx, comingPlayers, players1, players2, divideAttemptsCount, update);
    }

    @Override
    boolean preferNewOption(int current, int another) {
        return (another < current && another > 0) || (current == -1);
    }
}
