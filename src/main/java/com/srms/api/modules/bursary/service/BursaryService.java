package com.srms.api.modules.bursary.service;

import com.srms.api.modules.bursary.entity.BursaryApplication;
import com.srms.api.modules.bursary.entity.BursaryAward;
import com.srms.api.modules.bursary.entity.BursaryRenewal;
import com.srms.api.modules.bursary.repository.BursaryApplicationRepository;
import com.srms.api.modules.bursary.repository.BursaryRepository;
import com.srms.api.modules.bursary.repository.BursaryRenewalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class BursaryService {
    private final BursaryRepository repo;
    private final BursaryApplicationRepository applicationRepository;
    private final BursaryRenewalRepository renewalRepository;
    public List<BursaryAward> list(String schoolId) { return repo.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public BursaryAward create(String schoolId, BursaryAward b) { b.setSchoolId(schoolId); return repo.save(b); }
    public BursaryAward update(String schoolId, String id, BursaryAward updated) {
        BursaryAward b = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        b.setStatus(updated.getStatus()); b.setAmount(updated.getAmount()); b.setCoverage(updated.getCoverage());
        return repo.save(b);
    }

    public List<BursaryApplication> applications(String schoolId) { return applicationRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public BursaryApplication createApplication(String schoolId, BursaryApplication application) {
        application.setSchoolId(schoolId);
        if (application.getStatus() == null) application.setStatus("Submitted");
        return applicationRepository.save(application);
    }
    public BursaryApplication updateApplication(String schoolId, String id, BursaryApplication patch) {
        BursaryApplication application = applicationRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        if (patch.getStatus() != null) application.setStatus(patch.getStatus());
        if (patch.getReason() != null) application.setReason(patch.getReason());
        return applicationRepository.save(application);
    }

    public List<BursaryRenewal> renewals(String schoolId) { return renewalRepository.findBySchoolIdOrderByReviewDateAscCreatedAtDesc(schoolId); }
    public BursaryRenewal createRenewal(String schoolId, BursaryRenewal renewal) {
        renewal.setSchoolId(schoolId);
        return renewalRepository.save(renewal);
    }
    public BursaryRenewal updateRenewal(String schoolId, String id, BursaryRenewal patch) {
        BursaryRenewal renewal = renewalRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        if (patch.getStatus() != null) renewal.setStatus(patch.getStatus());
        if (patch.getAttendance() != null) renewal.setAttendance(patch.getAttendance());
        if (patch.getAcademics() != null) renewal.setAcademics(patch.getAcademics());
        return renewalRepository.save(renewal);
    }
}
