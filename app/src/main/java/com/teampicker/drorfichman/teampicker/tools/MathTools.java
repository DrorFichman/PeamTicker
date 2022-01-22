package com.teampicker.drorfichman.teampicker.tools;

import java.util.List;

public class MathTools {

    public static double getStdDevFromDiffs(List<Integer> list) {

        int total = 0;
        for (Integer diff : list) {
            total += Math.pow(diff, 2);
        }

        if (list.size() > 0) return Math.sqrt(total / list.size());
        else return -1;
    }

    /**
     * Returns a 0.35-1 value based on deviation from 0 to max
     */
    public static float getAlpha(int value, int max) {
        if (max == 0) return 1;
        value = Math.abs(value);
        max = Math.abs(max);
        float alpha = Math.abs((float) value / max);
        if (alpha > 1) return 1;
        if (alpha < 0.35) return (float) 0.35;
        return alpha;
    }

    /**
     * Returns a 0.35-1 value based on deviation from 50%
     */
    public static float getAlpha(int percentage) {
        float alpha = (float) (Math.abs(percentage - 50) * 2) / 100;
        if (alpha > 1) return 1;
        if (alpha < 0.35) return (float) 0.35;
        return alpha;
    }


    public static int getPercentageOf(int value, int max) {
        return getLimitedValue(value, 0, max) * 100 / max;
    }

    public static int getLimitedValue(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
