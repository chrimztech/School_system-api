package com.srms.api.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingBandDto {
    private int min;
    private int max;
    private String grade;
    private String description;
    private int points;
}
