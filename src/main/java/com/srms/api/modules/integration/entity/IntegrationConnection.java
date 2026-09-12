package com.srms.api.modules.integration.entity;

import com.srms.api.common.BaseEntity;
import com.srms.api.common.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "integration_connections", indexes = @Index(name = "idx_integration_connections_school_id", columnList = "school_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationConnection extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Deliberately no Java-level default (`= false`) here even though the column has one at the
    // DB level: a field initializer runs in every constructor Lombok generates, including the
    // plain no-args one Jackson uses to build the @RequestBody for PATCH /integrations/{code} —
    // so a JSON patch body that simply omits "connected" would deserialize to `false` instead of
    // `null`, and IntegrationService.merge()'s "only touch fields the caller actually sent" check
    // (`patch.getConnected() != null`) would then silently disconnect the integration on every
    // save of any other field (owner, webhook, credentials, ...). Leaving this unset means a
    // brand-new row must set it explicitly (every current creation path already does).
    @Column(columnDefinition = "boolean default false")
    private Boolean connected;

    private String status;
    private String owner;
    private String webhook;

    /** Human-readable outcome of the most recent real "Test connection" / live call — shown on
     * the Connection health tab so a "Degraded" chip has an actual reason attached. */
    @Column(columnDefinition = "TEXT")
    private String lastTestMessage;

    /** Merchant/account/client ID — appears on receipts and dashboards for most providers, not
     * secret-grade like the key/secret below, so kept in plain text. */
    private String accountId;

    /** API base URL — e.g. a sandbox vs. production host for the same provider. */
    private String baseUrl;

    /** Second endpoint URL some providers need (e.g. ZynlePay's separate payment-status URL) —
     * blank/unused for providers that only need one. */
    private String secondaryUrl;

    /** "sandbox" | "production" */
    private String environment;

    /** Encrypted at rest (AES-256/GCM) — see {@link EncryptedStringConverter}. Never returned to
     * the frontend in plaintext; the controller maps this to a masked view for reads. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String apiKey;

    /** Encrypted at rest — same treatment as {@link #apiKey}. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String apiSecret;
}
