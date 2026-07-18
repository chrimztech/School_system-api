package com.srms.api.modules.alumni.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "alumni_records", indexes = @Index(name = "idx_alumni_records_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlumniRecord extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String firstName;
    private String lastName;
    private String admissionNumber;
    private int graduationYear;
    private int lastGrade;
    private String currentEmployer;
    private String currentPosition;
    private String email;
    private String phone;
    private String location;
    private boolean updatedByUser;
    private String status; // ACTIVE, INACTIVE
    private String industrySector;
    private String highestQualification;
    private String qualificationsAchieved;
    private String linkedIn;
    private String engagementStatus; // Active, Mentor, Donor, Inactive
}
