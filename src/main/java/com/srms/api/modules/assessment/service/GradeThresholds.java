package com.srms.api.modules.assessment.service;

import java.util.List;

/** Compatibility helper for callers that already work with grading-band records. */
public final class GradeThresholds {
    private GradeThresholds() {}

    public static final List<GradingBand> DEFAULT_BANDS = List.of(
            new GradingBand(80, 100, "A", "Excellent", 1),
            new GradingBand(70, 79, "B+", "Very Good", 2),
            new GradingBand(60, 69, "B", "Good", 3),
            new GradingBand(50, 59, "C+", "Credit", 4),
            new GradingBand(40, 49, "C", "Satisfactory", 5),
            new GradingBand(30, 39, "D+", "Elementary", 6),
            new GradingBand(20, 29, "D", "Limited Achievement", 7),
            new GradingBand(0, 19, "E", "Unsatisfactory", 8)
    );

    public static String letterGrade(double percentage, List<GradingBand> bands) {
        List<GradingBand> effective = bands == null || bands.isEmpty() ? DEFAULT_BANDS : bands;
        double bounded = Math.max(0, Math.min(100, percentage));
        return effective.stream()
                .filter(band -> bounded >= band.min() && bounded < band.max() + 1)
                .findFirst()
                .map(GradingBand::grade)
                .orElse(effective.get(effective.size() - 1).grade());
    }
}
