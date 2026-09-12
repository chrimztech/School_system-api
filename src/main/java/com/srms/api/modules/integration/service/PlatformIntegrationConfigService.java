package com.srms.api.modules.integration.service;

import com.srms.api.modules.integration.dto.PlatformIntegrationConfigDto;
import com.srms.api.modules.integration.entity.PlatformIntegrationConfig;
import com.srms.api.modules.integration.repository.PlatformIntegrationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Lets a platform-level integration's credentials (payment gateway, bulk-SMS) be configured
 * from the Developer Console instead of requiring a redeploy with different environment
 * variables. Every client (ZynlePayClient, ZamtelSmsClient) still carries its original
 * @Value-injected properties as the fallback default — resolve()'s job is purely "does a
 * non-blank override exist in the database; if so use it, otherwise use what was already
 * there." A school that never touches this page keeps working exactly as before.
 */
@Service
@RequiredArgsConstructor
public class PlatformIntegrationConfigService {
    public static final String ZYNLEPAY = "ZYNLEPAY";
    public static final String ZAMTEL_SMS = "ZAMTEL_SMS";

    private final PlatformIntegrationConfigRepository repository;

    public List<PlatformIntegrationConfigDto> listMasked() {
        return repository.findAllByOrderByProviderAsc().stream().map(this::toMaskedDto).toList();
    }

    public PlatformIntegrationConfigDto getMasked(String provider) {
        return repository.findByProvider(provider)
                .map(this::toMaskedDto)
                .orElseGet(() -> {
                    PlatformIntegrationConfigDto dto = new PlatformIntegrationConfigDto();
                    dto.setProvider(provider);
                    dto.setEnabled(false);
                    dto.setApiKeySet(false);
                    return dto;
                });
    }

    public PlatformIntegrationConfigDto save(String provider, PlatformIntegrationConfigDto dto) {
        PlatformIntegrationConfig config = repository.findByProvider(provider)
                .orElse(PlatformIntegrationConfig.builder().provider(provider).build());
        if (dto.getEnabled() != null) config.setEnabled(dto.getEnabled());
        if (dto.getBaseUrl() != null) config.setBaseUrl(dto.getBaseUrl());
        if (dto.getSecondaryUrl() != null) config.setSecondaryUrl(dto.getSecondaryUrl());
        if (dto.getAccountId() != null) config.setAccountId(dto.getAccountId());
        if (dto.getClientId() != null) config.setClientId(dto.getClientId());
        // Only overwrite the stored key when a real, non-blank value was actually sent — the
        // frontend never has the real key to send back, only the masked hint, and must be able
        // to save every other field without that hint being mistaken for a new key.
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) config.setApiKey(dto.getApiKey());
        return toMaskedDto(repository.save(config));
    }

    private PlatformIntegrationConfigDto toMaskedDto(PlatformIntegrationConfig config) {
        PlatformIntegrationConfigDto dto = new PlatformIntegrationConfigDto();
        dto.setProvider(config.getProvider());
        dto.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
        dto.setBaseUrl(config.getBaseUrl());
        dto.setSecondaryUrl(config.getSecondaryUrl());
        dto.setAccountId(config.getAccountId());
        dto.setClientId(config.getClientId());
        String key = config.getApiKey();
        dto.setApiKeySet(key != null && !key.isBlank());
        dto.setApiKey(dto.isApiKeySet() ? "••••" + key.substring(Math.max(0, key.length() - 4)) : null);
        return dto;
    }

    /** Resolves one effective field for a provider: the stored override if non-blank, else the
     * client's own compiled-in default. Package-private-ish helper each client calls once per
     * field rather than duplicating this null/blank check everywhere. */
    public String resolve(String provider, java.util.function.Function<PlatformIntegrationConfig, String> field, String fallback) {
        Optional<PlatformIntegrationConfig> config = repository.findByProvider(provider);
        if (config.isEmpty() || !Boolean.TRUE.equals(config.get().getEnabled())) return fallback;
        String value = field.apply(config.get());
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
