package com.srms.api.modules.student.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
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
    private String photoUrl;

    public enum StudentStatus { active, inactive, transferred, graduated }
}