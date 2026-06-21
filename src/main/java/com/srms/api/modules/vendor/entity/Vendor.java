package com.srms.api.modules.vendor.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "vendors") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vendor extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String name;
    private String category;
    private String contactPerson;
    private String phone;
    private String email;
    private String tpin;
    private String status; // Active, Under review, Blacklisted
    private String contractExpiry;
    private String registrationNumber;
    private String vendorAddress;
    private String bankName;
    private String bankAccount;
    private String paymentTerms;
    private String serviceSpecializations;
    private String slaResponseTime;
    private String contractStartDate;
}
