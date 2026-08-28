package com.srms.api.modules.school.dto;

import com.srms.api.modules.assessment.dto.GradingBandDto;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolDto {
    private String id;
    private String name;
    private String shortCode;
    private String motto;
    private String district;
    private String province;
    private String type;
    private String ownership;
    private String category;
    private String gender;
    private String curriculum;
    private String languageOfInstruction;
    private String email;
    private String phone;
    private String altPhone;
    private String website;
    private String physicalAddress;
    private String poBox;
    private String city;
    private String postalCode;
    private String gpsCoordinates;
    private String headTeacher;
    private String headTeacherEmail;
    private String deputyHead;
    private String boardChair;
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
    private String fontFamily;
    private String logoUrl;
    private String faviconUrl;
    private String reportFooter;
    private String registrationNo;
    private String tpinNo;
    private String moeCode;
    private String examCentreNo;
    private Integer yearFounded;
    private String weekStart;
    private String gradingScale;
    private String smsSenderId;
    private String communicationsEmail;
    private String whatsappNumber;
    private String resultPublicationMode;
    private List<GradingBandDto> gradingBands;
    private Integer passMark;
    private String currency;
    private String bankName;
    private String bankAccount;
    private String bankBranch;
    private String termStart;
    private String termEnd;
    private Integer currentTerm;
    private Integer currentYear;
    private Integer totalStudents;
    private Integer totalTeachers;
    private Integer totalClasses;
    private String subscriptionStatus;
    private String planId;
    private String billingCycle;
    private Integer amount;
    private Integer campusLimit;
    private String nextInvoiceDate;
    private String renewalDate;
    private Integer learnerLimit;
    private Integer smsQuota;
    private Integer smsUsed;
    private String supportLevel;
    private String billingContact;
    private String notes;
    private Boolean offlineMode;
    private Boolean active;
    private String slug;
    private List<String> levels;
    private List<CampusDto> campuses;
    private Map<String, Boolean> features;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CampusDto {
        private String id;
        private String name;
        private String code;
        private String district;
        private String city;
        private String address;
        private String phone;
        private String status;
        private List<String> levels;
        private Integer studentCount;
        private Integer teacherCount;
    }
}
