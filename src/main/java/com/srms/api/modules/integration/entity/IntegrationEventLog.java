package com.srms.api.modules.integration.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per notable thing that happened for an integration — a test connection, a live action
 * (Zoom meeting created, Power BI snapshot published, ECZ sync attempted), or an inbound webhook
 * received from the provider. This is the "transaction/synchronisation history" and part of the
 * "audit trail" called for on each integration's setup screen — deliberately a flat event log
 * rather than a bespoke table per provider, since the shape (what happened, did it succeed, when,
 * with what detail) is identical across all eight providers.
 *
 * Real money-movement records (FeePayment rows) remain the source of truth for actual
 * transactions — this table exists to answer "what has this integration done/received recently",
 * not to duplicate financial ledgers.
 */
@Entity
@Table(name = "integration_event_logs", indexes = {
        @Index(name = "idx_integration_event_logs_lookup", columnList = "scope_type, school_id, provider_code, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationEventLog extends BaseEntity {
    public enum EventType { TEST, SYNC, PUBLISH, MEETING_CREATED, WEBHOOK, SAVE }

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private IntegrationConfig.ScopeType scopeType;

    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Column(name = "provider_code", nullable = false)
    private String providerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private Boolean success;

    @Column(columnDefinition = "TEXT")
    private String message;

    /** Free-form JSON for whatever detail the event carries (a webhook's raw payload, a created
     * meeting's join URL, ...) — not modeled per-provider for the same reason IntegrationConfig's
     * configurationJson isn't. */
    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    private String actor;
}
