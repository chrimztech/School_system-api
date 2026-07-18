package com.srms.api.modules.assessment.service;

import com.srms.api.modules.assessment.entity.Assessment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TermGradeServiceTest {
    private final TermGradeService service = new TermGradeService(
            null, null, null, null, null, null, null, null, null);

    @Test
    void combinedSchoolModeOverridesAnOlderSeparateAssessmentCycle() {
        Assessment assessment = Assessment.builder()
                .type(Assessment.AssessmentType.midterm)
                .reportingPeriod(Assessment.ReportingPeriod.MIDTERM)
                .build();

        assertEquals(Assessment.ReportingPeriod.COMBINED,
                service.effectivePeriod(assessment, "COMBINED"));
    }

    @Test
    void separateSchoolModeReclassifiesAnOlderCombinedAssessmentByType() {
        Assessment assessment = Assessment.builder()
                .type(Assessment.AssessmentType.midterm)
                .reportingPeriod(Assessment.ReportingPeriod.COMBINED)
                .build();

        assertEquals(Assessment.ReportingPeriod.MIDTERM,
                service.effectivePeriod(assessment, "SEPARATE"));
    }
}
