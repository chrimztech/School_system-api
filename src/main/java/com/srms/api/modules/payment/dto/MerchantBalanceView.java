package com.srms.api.modules.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MerchantBalanceView {
    private String merchantInformation;
    private String disbursementBalance;
    private String collectionBalance;
}
