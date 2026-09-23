package com.srms.api.modules.integration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.exception.BusinessException;
import com.srms.api.modules.integration.dto.IntegrationConfigSaveRequest;
import com.srms.api.modules.integration.dto.IntegrationConfigView;
import com.srms.api.modules.integration.entity.IntegrationConfig;
import com.srms.api.modules.integration.entity.IntegrationEventLog;
import com.srms.api.modules.integration.repository.IntegrationConfigRepository;
import com.srms.api.modules.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Single point of storage and access for every third-party integration's configuration, for both
 * scopes (see {@link IntegrationConfig}). Super-admin-only at the controller layer — this service
 * itself doesn't re-check the role, callers (IntegrationConfigController, the provider clients)
 * are trusted internal callers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IntegrationConfigService {
    private final IntegrationConfigRepository repository;
    private final IntegrationEventLogService eventLog;
    private final ObjectMapper objectMapper;
    private final SchoolRepository schoolRepository;

    @Value("${app.base-url:http://localhost:8090}")
    private String appBaseUrl;

    // ---- Provider registry — display name/category shown before a row exists yet ----
    public record ProviderMeta(String code, String displayName, String category, String callbackPath) {}

    public static final List<ProviderMeta> PROVIDERS = List.of(
            new ProviderMeta("momo", "MTN Mobile Money", "Payments", "/api/payments/callbacks/mtn-momo"),
            new ProviderMeta("airtel", "Airtel Money", "Payments", "/api/payments/callbacks/airtel-money"),
            new ProviderMeta("zynlepay", "ZynlePay", "Payments", "/api/public/payments/zynlepay/callback"),
            new ProviderMeta("sms", "Africa's Talking SMS", "Messaging", "/api/integrations/callbacks/africas-talking"),
            new ProviderMeta("ecz", "ECZ Sync", "Government", null),
            new ProviderMeta("powerbi", "Power BI", "Analytics", null),
            new ProviderMeta("google", "Google Workspace", "Identity", null),
            new ProviderMeta("zoom", "Zoom Education", "Productivity", "/api/integrations/callbacks/zoom")
    );

    private static ProviderMeta metaFor(String providerCode) {
        return PROVIDERS.stream().filter(p -> p.code().equals(providerCode)).findFirst()
                .orElseThrow(() -> new BusinessException("Unknown integration provider: " + providerCode));
    }

    // ---- Reads ----

    public List<IntegrationConfigView> list(IntegrationConfig.ScopeType scopeType, String schoolId) {
        String key = scopeType == IntegrationConfig.ScopeType.PLATFORM ? IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID : schoolId;
        Map<String, IntegrationConfig> existing = new LinkedHashMap<>();
        for (IntegrationConfig c : repository.findByScopeTypeAndSchoolIdOrderByDisplayNameAsc(scopeType, key)) {
            existing.put(c.getProviderCode(), c);
        }
        return PROVIDERS.stream()
                .map(meta -> existing.containsKey(meta.code())
                        ? toView(existing.get(meta.code()))
                        : emptyView(scopeType, key, meta))
                .toList();
    }

    public Optional<IntegrationConfigView> get(IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode) {
        String key = scopeType == IntegrationConfig.ScopeType.PLATFORM ? IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID : schoolId;
        return repository.findByScopeTypeAndSchoolIdAndProviderCode(scopeType, key, providerCode).map(this::toView);
    }

    private IntegrationConfigView emptyView(IntegrationConfig.ScopeType scopeType, String schoolId, ProviderMeta meta) {
        return new IntegrationConfigView(null, scopeType.name(), schoolId, meta.code(), meta.displayName(), meta.category(),
                null, false, IntegrationConfig.ConnectionStatus.NOT_CONFIGURED.name(), Map.of(), Map.of(),
                callbackUrlFor(meta, schoolId), null, null, null, null, null, null, null, null);
    }

    private String callbackUrlFor(ProviderMeta meta, String schoolId) {
        if (meta.callbackPath() == null) return null;
        return appBaseUrl + meta.callbackPath() + "/" + schoolId;
    }

    private IntegrationConfigView toView(IntegrationConfig c) {
        Map<String, Object> configuration = readConfigMap(c);
        Map<String, String> credentials = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : readCredentialMap(c).entrySet()) {
            credentials.put(e.getKey(), mask(e.getValue()));
        }
        ProviderMeta meta = metaFor(c.getProviderCode());
        return new IntegrationConfigView(
                c.getId(), c.getScopeType().name(), c.getSchoolId(), c.getProviderCode(), c.getDisplayName(),
                c.getCategory(), c.getEnvironment(), Boolean.TRUE.equals(c.getEnabled()), c.getConnectionStatus().name(),
                configuration, credentials, callbackUrlFor(meta, c.getSchoolId()),
                c.getLastTestedAt(), c.getLastSuccessAt(), c.getLastFailureAt(), c.getLastErrorMessage(),
                c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedBy(), c.getUpdatedAt());
    }

    private static String mask(String secret) {
        if (secret == null || secret.isBlank()) return null;
        int keep = Math.min(4, secret.length());
        return "••••" + secret.substring(secret.length() - keep);
    }

    // ---- Writes ----

    public IntegrationConfigView save(IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode,
                                       IntegrationConfigSaveRequest request, String actorId) {
        ProviderMeta meta = metaFor(providerCode);
        String key = scopeType == IntegrationConfig.ScopeType.PLATFORM ? IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID : schoolId;
        IntegrationConfig entity = repository.findByScopeTypeAndSchoolIdAndProviderCode(scopeType, key, providerCode)
                .orElseGet(() -> {
                    IntegrationConfig fresh = new IntegrationConfig();
                    fresh.setScopeType(scopeType);
                    fresh.setSchoolId(key);
                    fresh.setProviderCode(providerCode);
                    fresh.setConnectionStatus(IntegrationConfig.ConnectionStatus.NOT_CONFIGURED);
                    fresh.setCreatedBy(actorId);
                    return fresh;
                });

        entity.setDisplayName(request.displayName() != null ? request.displayName() : meta.displayName());
        entity.setCategory(request.category() != null ? request.category() : meta.category());
        if (request.environment() != null) entity.setEnvironment(request.environment());
        if (request.enabled() != null) entity.setEnabled(request.enabled());

        if (request.configuration() != null) {
            entity.setConfigurationJson(writeJson(request.configuration()));
        }

        if (request.credentials() != null && !request.credentials().isEmpty()) {
            Map<String, String> merged = new LinkedHashMap<>(readCredentialMap(entity));
            for (Map.Entry<String, String> e : request.credentials().entrySet()) {
                if (e.getValue() != null && !e.getValue().isBlank()) {
                    merged.put(e.getKey(), e.getValue());
                }
            }
            entity.setEncryptedCredentials(writeJson(merged));
        }

        entity.setUpdatedBy(actorId);
        IntegrationConfig saved = repository.save(entity);
        eventLog.record(scopeType, key, providerCode, IntegrationEventLog.EventType.SAVE, true, "Configuration saved", null, actorId);
        return toView(saved);
    }

    /** Adds or replaces one credential field's value from an uploaded file (base64-encoded by the
     * caller) — used for certificate-type credentials (Power BI, ECZ) where pasting PEM text by
     * hand is error-prone. Goes through the exact same encrypted-credentials JSON blob as every
     * other secret; a "real file upload" here means a genuine multipart upload interaction, not a
     * separate unencrypted file store. */
    public IntegrationConfigView saveCredentialFile(IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode,
                                                     String fieldKey, String base64Content, String actorId) {
        String key = scopeType == IntegrationConfig.ScopeType.PLATFORM ? IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID : schoolId;
        IntegrationConfig entity = repository.findByScopeTypeAndSchoolIdAndProviderCode(scopeType, key, providerCode)
                .orElseThrow(() -> new BusinessException("Save the rest of this integration's settings before uploading a credential file"));
        Map<String, String> merged = new LinkedHashMap<>(readCredentialMap(entity));
        merged.put(fieldKey, base64Content);
        entity.setEncryptedCredentials(writeJson(merged));
        entity.setUpdatedBy(actorId);
        IntegrationConfig saved = repository.save(entity);
        eventLog.record(scopeType, key, providerCode, IntegrationEventLog.EventType.SAVE, true, "Uploaded " + fieldKey, null, actorId);
        return toView(saved);
    }

    public void recordTestOutcome(IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode, boolean success, String message) {
        String key = scopeType == IntegrationConfig.ScopeType.PLATFORM ? IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID : schoolId;
        repository.findByScopeTypeAndSchoolIdAndProviderCode(scopeType, key, providerCode).ifPresent(entity -> {
            entity.setLastTestedAt(LocalDateTime.now());
            if (success) {
                entity.setLastSuccessAt(LocalDateTime.now());
                entity.setConnectionStatus(IntegrationConfig.ConnectionStatus.HEALTHY);
                entity.setLastErrorMessage(null);
            } else {
                entity.setLastFailureAt(LocalDateTime.now());
                entity.setConnectionStatus(IntegrationConfig.ConnectionStatus.DEGRADED);
                entity.setLastErrorMessage(message);
            }
            repository.save(entity);
        });
        eventLog.record(scopeType, key, providerCode, IntegrationEventLog.EventType.TEST, success, message, null, null);
    }

    /** Records a live action (Zoom meeting created, Power BI snapshot published, ECZ sync run) or
     * an inbound webhook — called by IntegrationActionsController and the webhook receivers. */
    public void recordEvent(IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode,
                             IntegrationEventLog.EventType eventType, boolean success, String message, String metadataJson, String actor) {
        eventLog.record(scopeType, schoolId, providerCode, eventType, success, message, metadataJson, actor);
    }

    public List<com.srms.api.modules.integration.dto.IntegrationEventView> recentEvents(IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode) {
        String key = scopeType == IntegrationConfig.ScopeType.PLATFORM ? IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID : schoolId;
        return eventLog.recent(scopeType, key, providerCode, 25);
    }

    // ---- Accessors used by the provider clients ----

    /** School-first-then-platform-fallback config value lookup — a school's own setting wins if
     * that school's row is enabled and the key is present; otherwise falls back to the platform
     * row (if enabled), otherwise empty. */
    public Optional<Object> resolveConfig(String providerCode, String schoolId, String key) {
        Optional<Object> schoolValue = enabledConfigMap(IntegrationConfig.ScopeType.SCHOOL, schoolId, providerCode)
                .map(m -> m.get(key)).filter(v -> v != null && !String.valueOf(v).isBlank());
        if (schoolValue.isPresent()) return schoolValue;
        return enabledConfigMap(IntegrationConfig.ScopeType.PLATFORM, IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID, providerCode)
                .map(m -> m.get(key)).filter(v -> v != null && !String.valueOf(v).isBlank());
    }

    public Optional<String> resolveCredential(String providerCode, String schoolId, String key) {
        Optional<String> schoolValue = enabledCredentialMap(IntegrationConfig.ScopeType.SCHOOL, schoolId, providerCode)
                .map(m -> m.get(key)).filter(v -> v != null && !v.isBlank());
        if (schoolValue.isPresent()) return schoolValue;
        return enabledCredentialMap(IntegrationConfig.ScopeType.PLATFORM, IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID, providerCode)
                .map(m -> m.get(key)).filter(v -> v != null && !v.isBlank());
    }

    /** True only if a school (not platform fallback) has this provider connected — used where a
     * feature must only activate for schools that deliberately opted in (e.g. choosing Africa's
     * Talking over the default SMS sender). */
    public boolean isSchoolEnabled(String providerCode, String schoolId) {
        return repository.findByScopeTypeAndSchoolIdAndProviderCode(IntegrationConfig.ScopeType.SCHOOL, schoolId, providerCode)
                .map(c -> Boolean.TRUE.equals(c.getEnabled())).orElse(false);
    }

    // ---- ZynlePay setup overview — which schools are running on their own merchant account vs.
    // riding the platform's shared fallback vs. having no working payment path at all. Lets the
    // platform team see who still needs to be onboarded onto their own account rather than
    // settling parent fee payments through the platform's own merchant account indefinitely. ----

    public record PaymentSetupRow(String schoolId, String schoolName, String status, String connectionStatus, LocalDateTime lastTestedAt) {}

    private static final String PAYMENT_PROVIDER_CODE = "zynlepay";

    public List<PaymentSetupRow> paymentSetupOverview() {
        boolean platformReady = zynlepayComplete(
                enabledConfigMap(IntegrationConfig.ScopeType.PLATFORM, IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID, PAYMENT_PROVIDER_CODE).orElse(Map.of()),
                enabledCredentialMap(IntegrationConfig.ScopeType.PLATFORM, IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID, PAYMENT_PROVIDER_CODE).orElse(Map.of()));

        Map<String, IntegrationConfig> ownConfigsBySchool = new LinkedHashMap<>();
        for (IntegrationConfig c : repository.findByScopeTypeAndProviderCode(IntegrationConfig.ScopeType.SCHOOL, PAYMENT_PROVIDER_CODE)) {
            ownConfigsBySchool.put(c.getSchoolId(), c);
        }

        return schoolRepository.findByActiveTrue().stream().map(school -> {
            IntegrationConfig own = ownConfigsBySchool.get(school.getId());
            boolean ownReady = own != null && Boolean.TRUE.equals(own.getEnabled())
                    && zynlepayComplete(readConfigMap(own), readCredentialMap(own));
            String status = ownReady ? "OWN_ACCOUNT" : platformReady ? "PLATFORM_FALLBACK" : "NOT_CONFIGURED";
            return new PaymentSetupRow(school.getId(), school.getName(), status,
                    own != null ? own.getConnectionStatus().name() : IntegrationConfig.ConnectionStatus.NOT_CONFIGURED.name(),
                    own != null ? own.getLastTestedAt() : null);
        }).toList();
    }

    private static boolean zynlepayComplete(Map<String, Object> configuration, Map<String, String> credentials) {
        Object merchantId = configuration.get("merchantId");
        return merchantId != null && !String.valueOf(merchantId).isBlank()
                && notBlank(credentials.get("apiId")) && notBlank(credentials.get("apiKey"));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private Optional<Map<String, Object>> enabledConfigMap(IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode) {
        return repository.findByScopeTypeAndSchoolIdAndProviderCode(scopeType, schoolId, providerCode)
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .map(this::readConfigMap);
    }

    private Optional<Map<String, String>> enabledCredentialMap(IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode) {
        return repository.findByScopeTypeAndSchoolIdAndProviderCode(scopeType, schoolId, providerCode)
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .map(this::readCredentialMap);
    }

    private Map<String, Object> readConfigMap(IntegrationConfig c) {
        if (c.getConfigurationJson() == null || c.getConfigurationJson().isBlank()) return Map.of();
        try {
            return objectMapper.readValue(c.getConfigurationJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Could not parse configuration JSON for {} {} {}: {}", c.getScopeType(), c.getSchoolId(), c.getProviderCode(), e.getMessage());
            return Map.of();
        }
    }

    private Map<String, String> readCredentialMap(IntegrationConfig c) {
        if (c.getEncryptedCredentials() == null || c.getEncryptedCredentials().isBlank()) return Map.of();
        try {
            return objectMapper.readValue(c.getEncryptedCredentials(), new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Could not parse credentials JSON for {} {} {}: {}", c.getScopeType(), c.getSchoolId(), c.getProviderCode(), e.getMessage());
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("Could not save configuration: " + e.getMessage());
        }
    }
}
