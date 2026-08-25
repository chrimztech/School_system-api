package com.srms.api.modules.school.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "schools")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class School extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
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

    @Column(columnDefinition = "TEXT")
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String faviconUrl;

    @Column(columnDefinition = "TEXT")
    private String reportFooter;
    private String registrationNo;
    private String tpinNo;
    private String moeCode;
    private String examCentreNo;
    private Integer yearFounded;
    private String weekStart;
    private String gradingScale;

    /**
     * The Zamtel sender ID SMS from this school should appear to come from (so a parent sees
     * the school's own name/short code, not a generic platform-wide one). Null/blank falls back
     * to zamtel.bulksms.sender-id — Zamtel requires each sender ID to be individually
     * registered and approved before it can be used, so this can't just default to the school's
     * actual name; it has to be whatever short id (typically the school's short code) has
     * actually been approved for them.
     */
    private String smsSenderId;
    private Integer passMark;
    private String resultPublicationMode; // "SEPARATE" | "COMBINED"

    @Column(columnDefinition = "TEXT")
    private String gradingBandsJson;
    private String currency;
    private String bankName;
    private String bankAccount;
    private String bankBranch;
    private String termStart;
    private String termEnd;
    private int currentTerm;
    private int currentYear;
    private int totalStudents;
    private int totalTeachers;
    private int totalClasses;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus subscriptionStatus;

    private String planId;
    private String billingCycle;
    private Integer subscriptionAmount;
    private Integer campusLimit;
    private String nextInvoiceDate;
    private String renewalDate;
    @Builder.Default
    private Integer learnerLimit = 0;
    @Builder.Default
    @Column(columnDefinition = "integer default 0") private Integer smsQuota = 0;
    @Builder.Default
    @Column(columnDefinition = "integer default 0") private Integer smsUsed = 0;
    private String supportLevel;
    private String billingContact;

    @Column(columnDefinition = "TEXT")
    private String subscriptionNotes;

    private Boolean offlineMode = false;

    @Column(columnDefinition = "TEXT")
    private String levelsJson;

    @Column(columnDefinition = "TEXT")
    private String campusesJson;

    @Column(columnDefinition = "TEXT")
    private String featuresJson;

    private boolean active = true;

    @Column(unique = true)
    private String slug; // e.g. "greenfields-secondary" — used for subdomain routing

    public enum SubscriptionStatus { trial, active, past_due, suspended }
}