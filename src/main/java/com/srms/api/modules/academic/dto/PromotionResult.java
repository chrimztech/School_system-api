package com.srms.api.modules.academic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PromotionResult {
    private int promoted;
    private int graduated;
}
