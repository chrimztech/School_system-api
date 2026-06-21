package com.srms.api.modules.teacher.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teachers")
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
    public enum TeacherStatus { active, inactive, on_leave, suspended, terminated }
}
