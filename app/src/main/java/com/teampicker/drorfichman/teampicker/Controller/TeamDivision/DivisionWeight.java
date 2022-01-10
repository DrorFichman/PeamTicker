package com.teampicker.drorfichman.teampicker.Controller.TeamDivision;

public class DivisionWeight {
    double grade;
    double chemistry;
    double stdDev;

    public double grade() {
        return grade / 100;
    }

    public String gradeDisplay() {
        return String.valueOf((int) grade);
    }

    public double chemistry() {
        return chemistry / 100;
    }

    public double stdDev() {
        return stdDev / 100;
    }

    public DivisionWeight(int g, int c, int s) {
        grade = g;
        chemistry = c;
        stdDev = s;
    }
}

