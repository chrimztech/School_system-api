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

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean connected = false;

    private String status;
    private String owner;
    private String webhook;

    /** Merchant/account/client ID — appears on receipts and dashboards for most providers, not
     * secret-grade like the key/secret below, so kept in plain text. */
    private String accountId;

    /** API base URL — e.g. a sandbox vs. production host for the same provider. */
    private String baseUrl;

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
