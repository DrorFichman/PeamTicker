package com.teampicker.drorfichman.teampicker.tools;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

import com.teampicker.drorfichman.teampicker.R;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

public class ColorHelper {

    enum ColorScheme {
        OrangeBlue(R.string.setting_team_color_scheme_orange_blue),
        BlackWhite(R.string.setting_team_color_scheme_black_white),
        OrangeYellow(R.string.setting_team_color_scheme_orange_yellow);

        int stringRes;

        ColorScheme(int res) {
            stringRes = res;
        }
    }

    @ColorInt
    public static int[] getTeamsColors(Context ctx) {
        String colorScheme = SettingsHelper.getColorScheme(ctx);

        int[] colors = new int[2];
        if (colorScheme.equals(ctx.getString(ColorScheme.BlackWhite.stringRes))) {
            colors[0] = ContextCompat.getColor(ctx, R.color.blackTeam);
            colors[1] = ContextCompat.getColor(ctx, R.color.whiteTeam);
        } else if (colorScheme.equals(ctx.getString(ColorScheme.OrangeYellow.stringRes))) {
            colors[0] = ContextCompat.getColor(ctx, R.color.orangeTeam);
            colors[1] = ContextCompat.getColor(ctx, R.color.yellowTeam);
        } else { // Orange - Blue
            colors[0] = ContextCompat.getColor(ctx, R.color.orangeTeam);
            colors[1] = ContextCompat.getColor(ctx, R.color.blueTeam);
        }
        return colors;
    }

    @DrawableRes
    public static int[] getTeamsIcons(Context ctx) {

        String colorScheme = SettingsHelper.getColorScheme(ctx);

        if (colorScheme.equals(ctx.getString(ColorScheme.BlackWhite.stringRes))) {
            return new int[]{R.drawable.circle_black,
                    R.drawable.circle_white,
                    R.drawable.circle_draw_blue};
        } else if (colorScheme.equals(ctx.getString(ColorScheme.OrangeYellow.stringRes))) {
            return new int[]{R.drawable.circle_orange,
                    R.drawable.circle_yellow,
                    R.drawable.circle_draw_gray};
        } else { // if Orange Blue
            return new int[]{R.drawable.circle_orange,
                    R.drawable.circle_blue,
                    R.drawable.circle_draw_blue};
        }
    }

    /**
     * Sets the appropriate color for the text view based on a percentage
     *
     * @param percentage - 0-100 value
     */
    public static void setColorAlpha(Context ctx, TextView textView, int percentage) {
        if (percentage > 50) textView.setTextColor(ContextCompat.getColor(ctx, R.color.high));
        else if (percentage < 50) textView.setTextColor(ContextCompat.getColor(ctx, R.color.low));
        else textView.setTextColor(Color.BLACK);

        float alpha = MathTools.getAlpha(percentage);
        textView.setTextColor(textView.getTextColors().withAlpha((int) (alpha * 255)));
    }

    /**
     * Sets the appropriate color for the text view based on a delta value out of a max value
     *
     * @param delta    - difference from 0
     * @param maxDelta - max difference
     */
    public static void setColorAlpha(Context ctx, TextView textView, int delta, int maxDelta) {
        if (delta > 0) textView.setTextColor(ContextCompat.getColor(ctx, R.color.high));
        else if (delta < 0) textView.setTextColor(ContextCompat.getColor(ctx, R.color.low));
        else textView.setTextColor(Color.BLACK);

        float alpha = MathTools.getAlpha(delta, maxDelta);
        textView.setTextColor(textView.getTextColors().withAlpha((int) (alpha * 255)));
    }
}
