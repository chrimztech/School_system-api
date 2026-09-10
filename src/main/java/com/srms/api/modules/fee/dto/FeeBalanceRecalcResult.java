package com.srms.api.modules.fee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeeBalanceRecalcResult {
    private int checked;
    private int updated;
}
