package com.srms.api.modules.assessment.dto;

import lombok.Data;

/**
 * One subject's already-known result for a past term/year — a transfer pupil's grades from a
 * previous school, or a paper report card being digitized — entered directly rather than
 * produced by the live capture → HOD verification → Careers Guidance publication pipeline
 * (that pipeline can never produce these: it requires an active class enrolment and live
 * Assessment/AssessmentResult rows for the term in question, neither of which exist for a
 * result that predates the pupil's time in this system). See TermGradeService.backfillResult.
 */
@Data
public class HistoricalResultDto {
    private String studentId;
    private String subjectName;
    private String term;
    private String academicYear;
    /** MIDTERM, END_TERM, or COMBINED — defaults to END_TERM when blank, since a historical
     * record is almost always the term's final result rather than a mid-term snapshot. */
    private String reportingPeriod;
    /** Percentage 0-100. Required — every reader of this data (report card averages, results
     * analysis) works off a percentage, so a bare letter grade with nothing behind it would
     * silently corrupt those aggregates. */
    private Double weightedTotal;
    /** Optional overrides — when omitted, derived from weightedTotal via the grading scale
     * named by classPhase (or the school's current default scale if classPhase is blank). Set
     * these directly when an old scale doesn't map cleanly onto either grading scale on file. */
    private String letterGrade;
    private String gradeDescription;
    private Integer gradePoints;
    /** "secondary_legacy" to grade this row against the pre-2023 scale; omit for the current
     * scale. Only meaningful when letterGrade is omitted (see above). */
    private String classPhase;
}
