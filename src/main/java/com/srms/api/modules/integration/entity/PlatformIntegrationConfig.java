package com.srms.api.modules.integration.entity;

import com.srms.api.common.BaseEntity;
import com.srms.api.common.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A platform-wide (not per-school) third-party integration's credentials, entered through the
 * Developer Console rather than baked in as env vars at deploy time — the payment gateway and
 * bulk-SMS provider used to require redeploying with different environment variables (e.g.
 * ZYNLEPAY_MERCHANT_ID, ZAMTEL_BULKSMS_API_KEY) to change. One row per provider ("ZYNLEPAY",
 * "ZAMTEL_SMS"); a school's own per-school integrations (IntegrationConnection) are a separate,
 * unrelated table.
 *
 * The generic column names below (accountId/clientId/apiKey) map differently per provider —
 * see PlatformIntegrationConfigService's per-client resolution methods for exactly which field
 * means what for a given provider. A provider row that doesn't exist, or whose fields are blank,
 * falls back to the application.properties/env-var defaults each client already had — this is
 * strictly additive, never a hard requirement to configure through the UI.
 */
@Entity
@Table(name = "platform_integration_configs", uniqueConstraints = @UniqueConstraint(columnNames = "provider"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformIntegrationConfig extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String provider;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean enabled = false;

    /** ZynlePay: baseUrl. Zamtel: base-url. */
    private String baseUrl;

    /** ZynlePay only: paymentStatusUrl. */
    private String secondaryUrl;

    /** ZynlePay: merchantId. Zamtel: sender-id. */
    private String accountId;

    /** ZynlePay only: apiId. */
    private String clientId;

    /** Encrypted at rest — see {@link EncryptedStringConverter}. ZynlePay: apiKey. Zamtel:
     * api-key. Never returned to the frontend in plaintext; the controller maps this to a
     * masked hint for reads, same treatment as IntegrationConnection's apiKey. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String apiKey;
}
