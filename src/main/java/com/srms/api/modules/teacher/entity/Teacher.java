package com.srms.api.modules.teacher.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teachers", indexes = @Index(name = "idx_teachers_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Teacher extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;
    @Column(nullable = false)
    private String staffNumber;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    private String email;
    private String phone;
    private String subject;
    private String qualification;
    private String department;
    private String dateJoined;
    private double salary;
    @Enumerated(EnumType.STRING)
    private TeacherStatus status;
    private String gender;
    private String nationalId;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String professionalLicenseNo;
    private Integer teachingExperienceYears;
    private String bankName;
    private String bankAccount;
    private String address;
    // Plain strings rather than enums for contractType/paymentMethod — the frontend's dropdown
    // labels here are free-form ("Permanent"/"Contract"/"Probation", "Bank transfer"/"Mobile
    // money (Airtel)"/...) and a Java enum's exact-match deserialization is exactly what broke
    // the payroll module's own contract-type field elsewhere in this app.
    private String contractType;
    private String contractEndDate;
    private String salaryBand;
    private String tpin;
    private String paymentMethod;
    private Boolean napsaEnrolled;

    @Column(columnDefinition = "TEXT")
    private String signatureUrl;

    public enum TeacherStatus { active, inactive, on_leave, suspended, terminated }
}
