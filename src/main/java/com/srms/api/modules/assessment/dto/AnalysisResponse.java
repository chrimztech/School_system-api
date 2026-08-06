package com.srms.api.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResponse {
    private String scope;
    private String scopeLabel;
    private int studentCount;
    private Double average;
    private Double passRate;
    private int passMarkUsed;
    private Map<String, Long> distribution;
    private List<SubjectBreakdown> bySubject;
}
