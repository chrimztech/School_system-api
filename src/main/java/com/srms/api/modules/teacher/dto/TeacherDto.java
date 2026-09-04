package com.srms.api.modules.teacher.dto;
import lombok.Data;
@Data
public class TeacherDto {
    private String firstName, lastName, email, phone, subject;
    private String qualification, department, dateJoined, gender, nationalId, status;
    private double salary;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String professionalLicenseNo;
    private Integer teachingExperienceYears;
    private String bankName;
    private String bankAccount;
    private String address;
    private String contractType;
    private String contractEndDate;
    private String salaryBand;
    private String tpin;
    private String paymentMethod;
    private Boolean napsaEnrolled;
}
