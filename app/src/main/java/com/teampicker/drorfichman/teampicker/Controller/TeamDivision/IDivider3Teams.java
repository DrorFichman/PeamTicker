package com.teampicker.drorfichman.teampicker.Controller.TeamDivision;

import android.content.Context;

import com.teampicker.drorfichman.teampicker.Data.Player;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Interface for dividing players into 3 teams
 */
public interface IDivider3Teams {

    void divide(Context ctx, @NonNull ArrayList<Player> comingPlayers,
                @NonNull List<Player> team1,
                @NonNull List<Player> team2,
                @NonNull List<Player> team3,
                int divideAttemptsCount,
                TeamDivision.onTaskInProgress update);
}

