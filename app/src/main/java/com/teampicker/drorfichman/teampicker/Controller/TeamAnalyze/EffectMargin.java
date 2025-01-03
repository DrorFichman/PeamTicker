package com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze;

import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerChemistry;

/**
 * The effect a collaborator has on a player, gathered from previous games together
 */
public class EffectMargin {
    int gamesWith;
    int winRateWith;
    int winRateMarginWith;
    int winsAndLosesWith;
    int winsWith;
    int successWith;
    int successMarginWith;

    EffectMargin(Player player, PlayerChemistry with) {
        gamesWith = with.statisticsWith.gamesCount;
        winsAndLosesWith = with.statisticsWith.getWinsAndLosesCount();
        winsWith = with.statisticsWith.wins;
        successWith = with.statisticsWith.successRate;
        winRateWith = with.statisticsWith.getWinRate();

        winRateMarginWith = with.statisticsWith.getWinRate() - player.statistics.getWinRate();
        successMarginWith = with.statisticsWith.successRate - player.statistics.successRate;
    }

    public int getWinRateMarginWith() {
        return winRateMarginWith;
    }

    public int getGamesWith() {
        return gamesWith;
    }

    public int getWinRateWith() {
        return winRateWith;
    }
}
