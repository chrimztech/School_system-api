package com.srms.api.modules.integration.service;

import com.srms.api.modules.integration.dto.IntegrationEventView;
import com.srms.api.modules.integration.entity.IntegrationConfig;
import com.srms.api.modules.integration.entity.IntegrationEventLog;
import com.srms.api.modules.integration.repository.IntegrationEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Records and lists the "what has this integration done recently" activity feed shown on each
 * integration's setup screen — test attempts, live actions (meetings created, snapshots
 * published, syncs run), and inbound webhooks. See IntegrationEventLog for why this is one flat
 * table rather than per-provider history tables. */
@Service
@RequiredArgsConstructor
@Transactional
public class IntegrationEventLogService {
    private final IntegrationEventLogRepository repository;

    public void record(IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode,
                        IntegrationEventLog.EventType eventType, boolean success, String message, String metadataJson, String actor) {
        repository.save(IntegrationEventLog.builder()
                .scopeType(scopeType)
                .schoolId(schoolId)
                .providerCode(providerCode)
                .eventType(eventType)
                .success(success)
                .message(message)
                .metadataJson(metadataJson)
                .actor(actor)
                .build());
    }

    public List<IntegrationEventView> recent(IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode, int limit) {
        return repository.findByScopeTypeAndSchoolIdAndProviderCodeOrderByCreatedAtDesc(
                        scopeType, schoolId, providerCode, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(IntegrationEventView::from)
                .getContent();
    }
}
