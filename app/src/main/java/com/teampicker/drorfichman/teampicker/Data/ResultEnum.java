package com.teampicker.drorfichman.teampicker.Data;

import android.graphics.Color;

import com.teampicker.drorfichman.teampicker.R;

import androidx.annotation.DrawableRes;

/**
 * Created by drorfichman on 10/3/16.
 */
public enum ResultEnum {
    Win("W", PlayerGamesDbHelper.WIN, Color.GREEN, R.drawable.circle_win),
    Lose("L", PlayerGamesDbHelper.LOSE, Color.RED, R.drawable.circle_lose),
    Tie("T", PlayerGamesDbHelper.TIE, Color.BLACK, R.drawable.circle_draw_blue),
    NA("?", PlayerGamesDbHelper.EMPTY_RESULT, Color.BLACK, R.drawable.circle_na),
    Missed("M", PlayerGamesDbHelper.MISSED_GAME, Color.GRAY, R.drawable.circle_na);

    ResultEnum(String s, int v, int c, int d) {
        sign = s;
        value = v;
        color = c;
        drawable = d;
    }

    private final int value;
    private final String sign;
    public final int color;
    private final int drawable;

    @DrawableRes
    public int getDrawable() {
        return drawable;
    }

    public String getChar() {
        return sign;
    }

    public int getValue() {
        if (value != PlayerGamesDbHelper.EMPTY_RESULT) {
            return value;
        }
        return 0;
    }

    public static ResultEnum getResultFromOrdinal(int res) {
        for (ResultEnum r : ResultEnum.values()) {
            if (r.getValue() == res) {
                return r;
            }
        }
        return null;
    }

    public static boolean isActive(ResultEnum res) {
        return res.value >= -1 && res.value <= 1;
    }

    public static boolean isActive(int gameResult) {
        return gameResult >= -1 && gameResult <= 1;
    }
}
