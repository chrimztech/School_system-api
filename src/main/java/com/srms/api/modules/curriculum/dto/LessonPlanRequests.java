package com.srms.api.modules.curriculum.dto;

import java.time.LocalDate;

public final class LessonPlanRequests {
    private LessonPlanRequests() {}

    public record Generate(String schemeId, String schemeWeekId, LocalDate lessonDate, Integer durationMinutes, String mode) {}

    /** classId and subjectName are only read when creating a lesson; updates keep the original. */
    public record Save(String classId, String subjectName, String topic, String objectives, String materials,
                       String introduction, String development, String conclusion, String evaluation,
                       String homework, LocalDate lessonDate, Integer durationMinutes, String status) {}
}
