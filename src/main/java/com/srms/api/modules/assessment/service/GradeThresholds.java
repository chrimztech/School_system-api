package com.srms.api.modules.assessment.service;

/** Single source of truth for letter-grade boundaries, shared by TermGradeService. */
public final class GradeThresholds {
    private GradeThresholds() {}

    public static String letterGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "F";
    }
}
