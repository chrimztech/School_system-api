package com.srms.api.modules.integration.entity;

import com.srms.api.common.BaseEntity;
import com.srms.api.common.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.time.LocalDateTime;

/**
 * Unified configuration record for every third-party integration (MTN Mobile Money, Airtel
 * Money, ECZ Sync, Africa's Talking SMS, Power BI, Google Workspace, Zoom, ZynlePay) — replaces
 * the earlier IntegrationConnection (per-school only, fixed columns) and PlatformIntegrationConfig
 * (platform only, fixed columns) with one model that supports both scopes and a provider-specific
 * field set per row, since each provider needs a genuinely different set of fields (MTN alone
 * needs ~15; the old model only had 5 generic columns).
 *
 * Non-secret settings live in {@link #configurationJson} as a flat JSON object; every credential
 * (API keys, client secrets, subscription keys, tokens, certificates, ...) lives together in
 * {@link #encryptedCredentials} as a JSON object encrypted as a single blob at rest (AES-256/GCM
 * via {@link EncryptedStringConverter}) — never split into per-field DB columns, so adding a new
 * secret field for a provider never needs a migration. Never echoed to the frontend in plaintext;
 * see IntegrationConfigView's per-key masking.
 *
 * schoolId is never actually null — a platform-wide row uses the sentinel
 * {@link #PLATFORM_SCOPE_SCHOOL_ID}, the same pattern BackupService uses for whole-system backups
 * (see Backup.schoolId / BackupService.PLATFORM_SCOPE). scopeType is stored alongside it purely so
 * a row's scope is self-describing without relying on callers to compare against the sentinel.
 */
@Entity
@Table(name = "integration_configs", indexes = {
        @Index(name = "idx_integration_configs_scope", columnList = "scope_type, school_id, provider_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationConfig extends BaseEntity {
    public static final String PLATFORM_SCOPE_SCHOOL_ID = "__PLATFORM__";

    public enum ScopeType { PLATFORM, SCHOOL }

    public enum ConnectionStatus { NOT_CONFIGURED, HEALTHY, DEGRADED, TESTING }

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private ScopeType scopeType;

    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Column(name = "provider_code", nullable = false)
    private String providerCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String category;
    private String environment;

    // Deliberately no Java-level default (see IntegrationConnection.connected's javadoc for the
    // exact bug this avoids) — every write to this entity goes through IntegrationConfigService's
    // explicit save-request DTO, never a raw @RequestBody-deserialized entity, so there's no
    // Jackson-default-constructor path that could silently clobber this on a partial update. Kept
    // non-defaulted anyway as a second line of defense.
    private Boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false)
    @Builder.Default
    private ConnectionStatus connectionStatus = ConnectionStatus.NOT_CONFIGURED;

    /** Non-secret, provider-specific settings as a flat JSON object (e.g. {"country":"ZM",
     * "currency":"ZMW","merchantName":"..."}). */
    @Column(name = "configuration_json", columnDefinition = "TEXT")
    private String configurationJson;

    /** Every credential for this provider as one JSON object, encrypted as a single blob. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "encrypted_credentials", columnDefinition = "TEXT")
    private String encryptedCredentials;

    @Column(name = "last_tested_at")
    private LocalDateTime lastTestedAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
