package com.teampicker.drorfichman.teampicker.Data;

public class StreakInfo {
    public final int length;
    public final String startDate;
    public final String endDate;
    public final long days;

    public StreakInfo(int length, String startDate, String endDate) {
        this.length = length;
        this.startDate = startDate;
        this.endDate = endDate;
        this.days = calculateDaysBetween(startDate, endDate);
    }

    private long calculateDaysBetween(String startDate, String endDate) {
        if (startDate == null || endDate == null) return 0;

        try {
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            return java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1; // +1 to include both start and end dates
        } catch (Exception e) {
            return 0;
        }
    }
} 