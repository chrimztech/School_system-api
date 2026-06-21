package com.srms.api.modules.admission.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "admission_applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdmissionApplication extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    private String applicationNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String preferredName;
    private String dateOfBirth;
    private String gender;
    private String nationality;
    private String birthCertificateNo;
    private int applyingForGrade;
    private String previousSchool;
    private String lastCompletedGrade;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;
    private String guardianName;
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
    private String source;
    private String priority;

    @Column(columnDefinition = "TEXT")
    private String medicalNotes;

    private String status; // PENDING, REVIEWING, ACCEPTED, REJECTED, WITHDRAWN
    private LocalDate submittedDate;
    private LocalDate decidedDate;
    private String enrolledStudentId;
    private String enrolledAdmissionNumber;
    private LocalDate enrolledDate;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
