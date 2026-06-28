package com.srms.api.modules.attendance.dto;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
@Data
public class AttendanceDto {
    private String classId;
    private String className;
    private LocalDate date;
    private List<AttendanceEntry> entries;
    @Data public static class AttendanceEntry {
        private String studentId;
        private String studentName;
        private String status;
        private String remarks;
    }
}
