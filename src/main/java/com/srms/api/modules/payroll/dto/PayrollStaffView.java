package com.srms.api.modules.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Unified read model for anyone eligible for payroll — merges HR's standalone StaffRecord
 * with Teacher records, since teachers hired via the Teachers page previously never appeared
 * here or in an actual payroll run at all (no linkage existed between the two entities). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollStaffView {
    private String id;
    private String source; // STAFF or TEACHER
    private String name;
    private String position;
    private String department;
    private String nationalId;
    private String bankName;
    private String accountNumber;
    private String tpin;
    private String paymentMethod;
    private Boolean napsaEnrolled;
    private BigDecimal salary;
    private String status;
}
