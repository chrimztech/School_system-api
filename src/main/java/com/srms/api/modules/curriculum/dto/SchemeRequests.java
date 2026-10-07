package com.srms.api.modules.curriculum.dto;

import java.util.List;

public final class SchemeRequests {
    private SchemeRequests() {}

    public record Create(String classId, String subjectName, String term, String academicYear) {}

    public record WeekInput(String topic, String subTopics, String objectives, String activities,
                            String resources, String assessment, String remarks) {}

    public record WeeksSave(List<WeekInput> weeks) {}

    public record Review(Boolean approve, String note) {}
}
