package com.srms.api.modules.policy.service;

import com.srms.api.modules.policy.entity.PolicyDocument;
import com.srms.api.modules.policy.repository.PolicyDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class PolicyDocumentService {
    private final PolicyDocumentRepository repo;

    public List<PolicyDocument> list(String schoolId) {
        return repo.findBySchoolIdOrderByUpdatedAtDesc(schoolId);
    }

    public PolicyDocument create(String schoolId, PolicyDocument doc) {
        doc.setSchoolId(schoolId);
        if (doc.getStatus() == null) doc.setStatus("Review");
        return repo.save(doc);
    }

    public PolicyDocument update(String schoolId, String id, PolicyDocument updated) {
        PolicyDocument doc = repo.findById(id).filter(d -> d.getSchoolId().equals(schoolId)).orElseThrow();
        if (updated.getStatus() != null) doc.setStatus(updated.getStatus());
        return repo.save(doc);
    }

    public void delete(String schoolId, String id) {
        repo.findById(id).filter(d -> d.getSchoolId().equals(schoolId)).ifPresent(repo::delete);
    }
}
