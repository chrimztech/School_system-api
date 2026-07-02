package com.srms.api.modules.payment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MomoPaymentRequest {
    private String studentId;
    private double amount;
    private String phoneNumber;
}
