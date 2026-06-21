package com.srms.api.modules.compliance.service;

import com.srms.api.modules.compliance.entity.ComplianceItem;
import com.srms.api.modules.compliance.repository.ComplianceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class ComplianceService {
    private final ComplianceRepository repo;
    public List<ComplianceItem> list(String schoolId) { return repo.findBySchoolIdOrderByDueDateAsc(schoolId); }
    public ComplianceItem create(String schoolId, ComplianceItem c) { c.setSchoolId(schoolId); return repo.save(c); }
    public ComplianceItem update(String schoolId, String id, ComplianceItem updated) {
        ComplianceItem c = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        c.setStatus(updated.getStatus()); c.setNotes(updated.getNotes()); c.setDueDate(updated.getDueDate());
        return repo.save(c);
    }
}
