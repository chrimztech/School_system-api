package com.srms.api.modules.exam.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "gce_candidates", indexes = @Index(name = "idx_gce_candidates_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GceCandidate extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String examNumber;
    @Column(nullable = false) private String firstName;
    private String middleName;
    @Column(nullable = false) private String lastName;
    private String gender;
    private String dateOfBirth;
    private String nrc;
    private String grade;
    @Column(columnDefinition = "TEXT") private String subjects;
    private String centerNumber;
    private String phone;
    private String email;
    @Column(columnDefinition = "TEXT") private String address;
    private String previousSchool;
    @Builder.Default private String status = "REGISTERED";
}
