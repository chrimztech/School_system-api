package com.srms.api.modules.timetable.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "timetable_slots") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimetableSlot extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String classId;
    private String className;
    private String dayOfWeek; // MONDAY...FRIDAY
    private String startTime;
    private String endTime;
    private int period;
    private String subjectId;
    private String subjectName;
    private String teacherId;
    private String teacherName;
    private String room;
    private String academicYear;
    private int term;
}
