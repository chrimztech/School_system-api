package com.srms.api.modules.assessment.service;

import java.util.List;

/** Compatibility helper for callers that already work with grading-band records. */
public final class GradeThresholds {
    private GradeThresholds() {}

    public static final List<GradingBand> DEFAULT_BANDS = List.of(
            new GradingBand(75, 100, "1", "DISTINCTION", 1),
            new GradingBand(70, 74, "2", "DISTINCTION", 2),
            new GradingBand(65, 69, "3", "MERIT", 3),
            new GradingBand(60, 64, "4", "MERIT", 4),
            new GradingBand(55, 59, "5", "CREDIT", 5),
            new GradingBand(50, 54, "6", "CREDIT", 6),
            new GradingBand(45, 49, "7", "SATISFACTORY", 7),
            new GradingBand(40, 44, "8", "SATISFACTORY", 8),
            new GradingBand(0, 39, "9", "UNSATISFACTORY", 9)
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
