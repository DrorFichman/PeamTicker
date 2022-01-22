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
        OrangeBlue(R.string.setting_team_color_scheme_color_1),
        BlackWhite(R.string.setting_team_color_scheme_color_2);

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
        } else { // if BlueOrange
            colors[0] = ContextCompat.getColor(ctx, R.color.orangeTeam);
            colors[1] = ContextCompat.getColor(ctx, R.color.blueTeam);
        }
        return  colors;
    }

    @DrawableRes
    public static int[] getTeamsIcons(Context ctx){
        String colorScheme = SettingsHelper.getColorScheme(ctx);

        int[] icons = new int[2];
        if (colorScheme.equals(ctx.getString(ColorScheme.BlackWhite.stringRes))) {
            icons[0] = R.drawable.circle_black;
            icons[1] = R.drawable.circle_white;
        } else { // if BlueOrange
            icons[0] = R.drawable.circle_orange;
            icons[1] = R.drawable.circle_blue;
        }
        return icons;
    }

    /**
     * Sets the appropriate color for the text view based on a percentage
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
     * @param delta - difference from 0
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
