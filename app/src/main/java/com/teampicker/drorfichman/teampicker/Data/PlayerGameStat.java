package com.teampicker.drorfichman.teampicker.Data;

import com.google.firebase.database.Exclude;
import com.teampicker.drorfichman.teampicker.tools.DateHelper;

import java.io.Serializable;
import java.util.Date;

public class PlayerGameStat implements Serializable {

    public ResultEnum result;
    public int grade;
    public String gameDateString;
    public boolean isMVP;

    public PlayerGameStat(ResultEnum res, int currGrade, String date) {
        this(res, currGrade, date, false);
    }

    public PlayerGameStat(ResultEnum res, int currGrade, String date, boolean mvp) {
        result = res;
        grade = currGrade;
        gameDateString = date;
        isMVP = mvp;
    }

    @Exclude
    public Date getDate() {
        return DateHelper.getDate(this.gameDateString);
    }
}
