package com.srms.api.modules.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentStatusView {
    private String status;
    private double amount;
    private String studentName;
    private String referenceNumber;
}
