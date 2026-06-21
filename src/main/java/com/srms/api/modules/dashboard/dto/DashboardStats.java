package com.srms.api.modules.dashboard.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class DashboardStats {
    private int totalStudents;
    private int totalTeachers;
    private int totalClasses;
    private AttendanceStats attendanceToday;
    private FeeStats fees;
    private List<PhaseEnrolment> enrolmentByPhase;
    private List<TrendPoint> feeTrend;
    private List<TrendPoint> attendanceTrend;

    @Data @Builder public static class AttendanceStats {
        private int present, absent, late;
        private double rate;
    }
    @Data @Builder public static class FeeStats {
        private long collected, outstanding;
        private double collectionRate;
        private String currency;
    }
    @Data @Builder public static class PhaseEnrolment {
        private String phase;
        private int count;
    }
    @Data @Builder public static class TrendPoint {
        private String label;
        private double value;
    }
}
