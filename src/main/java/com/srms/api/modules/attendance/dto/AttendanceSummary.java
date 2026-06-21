package com.srms.api.modules.attendance.dto;
import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data @Builder
public class AttendanceSummary {
    private int present, absent, late, excused, total;
    private double rate;
    private List<ClassAttendance> byClass;
    @Data @Builder public static class ClassAttendance {
        private String className;
        private int present, total;
        private double rate;
    }
}
