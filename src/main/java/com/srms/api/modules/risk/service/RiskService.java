package com.srms.api.modules.risk.service;

import com.srms.api.modules.risk.entity.RiskEntry;
import com.srms.api.modules.risk.repository.RiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class RiskService {
    private final RiskRepository repo;
    public List<RiskEntry> list(String schoolId) { return repo.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public RiskEntry create(String schoolId, RiskEntry r) { r.setSchoolId(schoolId); return repo.save(r); }
    public RiskEntry update(String schoolId, String id, RiskEntry updated) {
        RiskEntry r = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        r.setStatus(updated.getStatus()); r.setLikelihood(updated.getLikelihood()); r.setImpact(updated.getImpact());
        r.setMitigation(updated.getMitigation()); r.setOwner(updated.getOwner());
        return repo.save(r);
    }
}
