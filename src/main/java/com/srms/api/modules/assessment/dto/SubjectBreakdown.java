package com.srms.api.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectBreakdown {
    private String subjectName;
    private int studentCount;
    private Double average;
    private Double passRate;
}
