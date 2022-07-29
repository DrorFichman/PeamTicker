package com.teampicker.drorfichman.teampicker.Data;

import com.google.firebase.database.Exclude;
import com.teampicker.drorfichman.teampicker.Controller.Search.FilterView;
import com.teampicker.drorfichman.teampicker.Controller.Sort.Sortable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.Nullable;

/**
 * Created by drorfichman on 7/27/16.
 */
public class Player extends Sortable implements Serializable, Comparable, FilterView.Filterable {
    public String mName;
    public String msgDisplayName;
    public int mGrade;
    public int mBirthYear;
    public int mBirthMonth;
    public int mBirthDay;
    private int mAge;

    public boolean isComing;
    public boolean archived;

    public boolean isGK;
    public boolean isDefender;
    public boolean isPlaymaker;
    public boolean isUnbreakable;
    public boolean isExtra;

    public Player() {
    }

    @Exclude
    public ArrayList<PlayerGameStat> results = new ArrayList<>();
    @Exclude
    public StatisticsData statistics;

    private static final int MAX_RECENT_GAMES_COUNT = 10;

    @Exclude
    public int gameResult;

    public Player(String name, int grade) {
        mName = name;
        mGrade = grade;
    }

    @Override
    public String toString() {
        return mName;
    }

    @Exclude
    public int getSuggestedGrade() {
        if (results == null || results.size() < 5) {
            return mGrade;
        } else if (Math.abs(getAverageGrade() - mGrade) > 1) {
            return mGrade;
        } else {
            int suggestedGrade = getAverageGrade() + getSuccess() / 2;
            return Math.max(Math.min(suggestedGrade, 99), 1);
        }
    }

    @Exclude
    public int getSuggestedGradeDiff() {
        return getSuggestedGrade() - mGrade;
    }

    private int getAverageGrade() {
        if (results != null && results.size() > 0) {
            int gameCount = 0;
            int historicGrade = 0;
            for (PlayerGameStat r : results) {
                if (r != null) {
                    historicGrade += r.grade;
                    gameCount++;
                }
                if (gameCount > MAX_RECENT_GAMES_COUNT) {
                    break;
                }
            }
            return historicGrade / gameCount;
        } else {
            return mGrade;
        }
    }

    @Exclude
    public int getSuccess() {
        int gameCount = 0;
        int value = 0;
        for (PlayerGameStat r : results) {
            if (r != null) {
                int val = r.result.getValue();
                if (val <= 1 && val >= -1) {
                    value += r.result.getValue();
                    gameCount++;
                }
            }
            if (gameCount > MAX_RECENT_GAMES_COUNT) {
                break;
            }
        }
        return value;
    }

    @Exclude
    public void setStatistics(StatisticsData statistics) {
        this.statistics = statistics;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Player) {
            Player p2 = (Player) o;
            if (this.mGrade > p2.mGrade) {
                return -1;
            } else if (this.mGrade == p2.mGrade) {
                return 0;
            } else {
                return 1;
            }
        }

        return -1;
    }

    public int getAge() {
        if (mAge > 0)
            return mAge;
        else if (mBirthYear > 0) {
            int currYear = Calendar.getInstance().get(Calendar.YEAR);
            int currMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;

            return (currYear - mBirthYear) + (currMonth > mBirthMonth ? 0 : -1);
        } else {
            return -1;
        }
    }

    public void setAge(int age) {
        mAge = age;
    }

    public boolean hasAttributes() {
        return isGK || isPlaymaker || isDefender || isUnbreakable || isExtra;
    }

    @Exclude
    public String getAttributes() {
        if (!hasAttributes()) return "";

        String attributes = "";
        if (isUnbreakable) attributes += "B,";
        if (isGK) attributes += "GK,";
        if (isPlaymaker) attributes += "PM,";
        if (isDefender) attributes += "D,";
        if (isExtra) attributes += "E,";
        return attributes.substring(0, attributes.length() - 1);
    }

    public boolean isAttribute(PlayerAttribute attribute) {
        if (attribute != null) {
            switch (attribute) {
                case isDefender:
                    return isDefender;
                case isGK:
                    return isGK;
                case isPlaymaker:
                    return isPlaymaker;
                case isUnbreakable:
                    return isUnbreakable;
                case isExtra:
                    return isExtra;
            }
        }
        return false;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Player && this.mName != null) {
            return this.mName.equals(((Player) obj).mName);
        }
        return super.equals(obj);
    }

    //region Sortable
    @Override
    public String name() {
        return mName;
    }

    @Override
    public int grade() {
        return mGrade;
    }

    @Override
    public int suggestedGrade() {
        return getSuggestedGradeDiff();
    }

    @Override
    public int age() {
        return getAge();
    }

    @Override
    public String attributes() {
        return getAttributes();
    }

    @Override
    public boolean coming() {
        return isComing;
    }

    @Override
    public int winRate() {
        return statistics.getWinRate();
    }

    @Override
    public int games() {
        return statistics.gamesCount;
    }

    @Override
    public int success() {
        return statistics.successRate;
    }
    //endregion

    //region Filterable
    @Override
    public String filterBy() {
        return mName;
    }
    //endregion
}
