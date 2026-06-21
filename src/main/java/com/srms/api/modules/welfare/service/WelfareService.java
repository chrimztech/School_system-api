package com.srms.api.modules.welfare.service;

import com.srms.api.modules.welfare.entity.CounselingSession;
import com.srms.api.modules.welfare.entity.WelfareCase;
import com.srms.api.modules.welfare.repository.CounselingSessionRepository;
import com.srms.api.modules.welfare.repository.WelfareCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class WelfareService {
    private final WelfareCaseRepository caseRepo;
    private final CounselingSessionRepository sessionRepo;

    public List<WelfareCase> listCases(String schoolId) { return caseRepo.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public WelfareCase createCase(String schoolId, WelfareCase c) { c.setSchoolId(schoolId); return caseRepo.save(c); }
    public WelfareCase updateCase(String schoolId, String id, WelfareCase updated) {
        WelfareCase c = caseRepo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        c.setStatus(updated.getStatus()); c.setLastContact(updated.getLastContact()); c.setAssignedTo(updated.getAssignedTo());
        return caseRepo.save(c);
    }

    public List<CounselingSession> listSessions(String schoolId) { return sessionRepo.findBySchoolIdOrderBySessionDateDesc(schoolId); }
    public CounselingSession createSession(String schoolId, CounselingSession s) { s.setSchoolId(schoolId); return sessionRepo.save(s); }
}
