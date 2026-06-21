package com.srms.api.modules.attendance.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
@Entity @Table(name = "attendance_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceRecord extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String studentId;
    private String studentName;
    private String classId;
    private String className;
    @Column(nullable = false) private LocalDate date;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AttendanceStatus status;
    private String remarks;
    private String teacherId;
    public enum AttendanceStatus { present, absent, late, excused }
}
