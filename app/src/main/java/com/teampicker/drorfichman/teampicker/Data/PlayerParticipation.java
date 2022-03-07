package com.teampicker.drorfichman.teampicker.Data;

import com.teampicker.drorfichman.teampicker.Controller.Search.FilterView;
import com.teampicker.drorfichman.teampicker.Controller.Sort.Sortable;

import java.io.Serializable;

public class PlayerParticipation extends Sortable implements Serializable, FilterView.Filterable {

    public String mName;
    public String mParticipatedWith;

    public StatisticsData statisticsWith = new StatisticsData();
    public StatisticsData statisticsVs = new StatisticsData();

    //region Sortable
    @Override
    public String name() {
        return mName;
    }

    @Override
    public int gamesWithCount() {
        return statisticsWith.gamesCount;
    }

    @Override
    public int successWith() {
        return statisticsWith.successRate;
    }

    @Override
    public int winRateWith() {
        return statisticsWith.getWinRate();
    }

    @Override
    public int gamesVsCount() {
        return statisticsVs.gamesCount;
    }

    @Override
    public int successVs() {
        return statisticsVs.successRate;
    }

    @Override
    public int winRateVs() {
        return statisticsVs.getWinRate();
    }
    //endregion

    //region Filterable
    @Override
    public String filterBy() {
        return mName;
    }
    //endregion
}
