package com.srms.api.modules.student.dto;

import lombok.Data;

@Data
public class StudentDto {
    private String firstName;
    private String middleName;
    private String lastName;
    private String preferredName;
    private Integer grade;
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
    private String medicalConditions;
    private String allergies;
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
    private String guardianAddress;
    private String emergencyContactName;
    private String emergencyContactRelationship;
    private String emergencyContactPhone;
    private String status;
    private String boardingStatus;
    private Boolean needsTransport;
}