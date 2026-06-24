package com.srms.api.modules.audit.service;

import com.srms.api.modules.audit.entity.AuditEvent;
import com.srms.api.modules.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class AuditService {
    private final AuditEventRepository repo;

    public List<AuditEvent> findAll(String schoolId) { return repo.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public AuditEvent create(String schoolId, AuditEvent event) {
        event.setSchoolId(schoolId);
        if (event.getSeverity() == null) event.setSeverity("info");
        return repo.save(event);
    }
}
