package com.srms.api.modules.ptc.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "ptc_meetings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PtcMeeting extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String meetingDate;
    private String term;
    private String academicYear;
    @Column(columnDefinition = "TEXT") private String agenda;
    private int attendeesCount;
    @Column(columnDefinition = "TEXT") private String minutes;
    @Column(columnDefinition = "TEXT") private String decisions;
    @Builder.Default private String status = "SCHEDULED"; // SCHEDULED, HELD, CANCELLED
    @Builder.Default private boolean published = false;
}
