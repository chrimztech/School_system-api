package com.srms.api.modules.curriculum.dto;

import java.util.List;

public record TopicsSaveRequest(String subjectName, Integer grade, String term, List<TopicInput> topics) {
    public record TopicInput(String topic, String subTopics, String objectives, String activities, String resources, String assessment) {}
}
