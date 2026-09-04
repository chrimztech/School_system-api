package com.srms.api.modules.integration.service;

import com.srms.api.modules.integration.entity.IntegrationConnection;
import com.srms.api.modules.integration.repository.IntegrationConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class IntegrationService {
    private final IntegrationConnectionRepository integrationConnectionRepository;

    public List<IntegrationConnection> list(String schoolId) {
        return integrationConnectionRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId);
    }

    public IntegrationConnection createOrConnect(String schoolId, IntegrationConnection connection) {
        IntegrationConnection existing = connection.getCode() == null ? null
                : integrationConnectionRepository.findBySchoolIdAndCode(schoolId, connection.getCode()).orElse(null);

        if (existing != null) {
            merge(existing, connection);
            return integrationConnectionRepository.save(existing);
        }

        connection.setSchoolId(schoolId);
        return integrationConnectionRepository.save(connection);
    }

    public IntegrationConnection update(String schoolId, String code, IntegrationConnection patch) {
        IntegrationConnection connection = integrationConnectionRepository.findBySchoolIdAndCode(schoolId, code).orElseThrow();
        merge(connection, patch);
        return integrationConnectionRepository.save(connection);
    }

    private void merge(IntegrationConnection target, IntegrationConnection patch) {
        if (patch.getCode() != null) target.setCode(patch.getCode());
        if (patch.getName() != null) target.setName(patch.getName());
        if (patch.getCategory() != null) target.setCategory(patch.getCategory());
        if (patch.getDescription() != null) target.setDescription(patch.getDescription());
        if (patch.getConnected() != null) target.setConnected(patch.getConnected());
        if (patch.getStatus() != null) target.setStatus(patch.getStatus());
        if (patch.getOwner() != null) target.setOwner(patch.getOwner());
        if (patch.getWebhook() != null) target.setWebhook(patch.getWebhook());
        if (patch.getAccountId() != null) target.setAccountId(patch.getAccountId());
        if (patch.getBaseUrl() != null) target.setBaseUrl(patch.getBaseUrl());
        if (patch.getEnvironment() != null) target.setEnvironment(patch.getEnvironment());
        // Left blank on the form (and therefore omitted from the request body) means "don't
        // change the saved credential" — exactly the null-safe behavior every other field here
        // already has. There's no separate "clear the key" affordance yet.
        if (patch.getApiKey() != null) target.setApiKey(patch.getApiKey());
        if (patch.getApiSecret() != null) target.setApiSecret(patch.getApiSecret());
    }
}
