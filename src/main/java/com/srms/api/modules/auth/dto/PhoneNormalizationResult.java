package com.srms.api.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PhoneNormalizationResult {
    private int scanned;
    private int updated;
    private List<String> conflicts;
}
