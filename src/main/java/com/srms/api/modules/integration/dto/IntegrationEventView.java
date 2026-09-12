package com.srms.api.modules.integration.dto;

import com.srms.api.modules.integration.entity.IntegrationEventLog;

import java.time.LocalDateTime;

public record IntegrationEventView(
        String id,
        String eventType,
        boolean success,
        String message,
        String metadata,
        String actor,
        LocalDateTime createdAt
) {
    public static IntegrationEventView from(IntegrationEventLog e) {
        return new IntegrationEventView(e.getId(), e.getEventType().name(), Boolean.TRUE.equals(e.getSuccess()),
                e.getMessage(), e.getMetadataJson(), e.getActor(), e.getCreatedAt());
    }
}
