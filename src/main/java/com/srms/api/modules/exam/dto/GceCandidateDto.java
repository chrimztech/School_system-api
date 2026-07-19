package com.srms.api.modules.exam.dto;

import lombok.Data;

@Data
public class GceCandidateDto {
    private String examNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private String dateOfBirth;
    private String nrc;
    private String grade;
    private String subjects;
    private String centerNumber;
    private String phone;
    private String email;
    private String address;
    private String previousSchool;
    private String status;
}
