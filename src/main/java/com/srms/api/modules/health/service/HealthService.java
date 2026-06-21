package com.srms.api.modules.health.service;

import com.srms.api.modules.health.entity.HealthRecord;
import com.srms.api.modules.health.entity.HealthVisit;
import com.srms.api.modules.health.repository.HealthRecordRepository;
import com.srms.api.modules.health.repository.HealthVisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class HealthService {
    private final HealthRecordRepository recordRepo;
    private final HealthVisitRepository visitRepo;

    public List<HealthRecord> getAllRecords(String schoolId) { return recordRepo.findBySchoolId(schoolId); }
    public HealthRecord getRecord(String schoolId, String id) { return recordRepo.findById(id).filter(r -> r.getSchoolId().equals(schoolId)).orElseThrow(); }
    public HealthRecord getRecordByStudent(String schoolId, String studentId) { return recordRepo.findBySchoolIdAndStudentId(schoolId, studentId).orElse(null); }
    public HealthRecord saveRecord(String schoolId, HealthRecord record) { record.setSchoolId(schoolId); return recordRepo.save(record); }

    public List<HealthVisit> getAllVisits(String schoolId) { return visitRepo.findBySchoolIdOrderByVisitDateDesc(schoolId); }
    public HealthVisit createVisit(String schoolId, HealthVisit visit) {
        visit.setSchoolId(schoolId);
        if (visit.getVisitDate() == null) visit.setVisitDate(LocalDate.now());
        return visitRepo.save(visit);
    }
}
