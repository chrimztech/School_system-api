package com.srms.api.modules.student.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students", indexes = @Index(name = "idx_students_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Student extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false, unique = true)
    private String admissionNumber;

    @Column(nullable = false)
    private String firstName;

    private String middleName;

    @Column(nullable = false)
    private String lastName;

    private String preferredName;
    private int grade;
    private String section;
    private String admissionDate;
    private String dateOfBirth;
    private String gender;
    private String nationality;
    private String nationalId;
    private String birthCertificateNo;
    private String studentPhone;
    private String studentEmail;
    private String religion;
    private String bloodGroup;

    @Column(columnDefinition = "TEXT")
    private String medicalConditions;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;
    private String guardian;
    private String guardianRelationship;
    private String guardianPhone;
    private String guardianAltPhone;
    private String guardianEmail;
    private String guardianOccupation;
    private String guardianWorkplace;
    private String guardianNationalId;

    @Column(columnDefinition = "TEXT")
    private String guardianAddress;

    private String emergencyContactName;
    private String emergencyContactRelationship;
    private String emergencyContactPhone;

    @Enumerated(EnumType.STRING)
    private StudentStatus status;

    private double feeBalance;

    @Column(columnDefinition = "TEXT")
    private String photoUrl;

    /** "DAY" or "BOARDING" — plain string like the rest of this entity's loosely-typed
     *  fields, not an enum, since it's display/filter data rather than something validated. */
    private String boardingStatus;
    private boolean needsTransport;

    public enum StudentStatus { active, inactive, transferred, graduated }
}