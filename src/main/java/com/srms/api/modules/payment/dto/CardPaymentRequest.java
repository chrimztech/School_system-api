package com.srms.api.modules.payment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardPaymentRequest {
    private String studentId;
    private double amount;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zipCode;
}
