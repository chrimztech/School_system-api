package com.srms.api.modules.alumni.service;

import com.srms.api.modules.alumni.entity.AlumniRecord;
import com.srms.api.modules.alumni.repository.AlumniRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class AlumniService {
    private final AlumniRepository repo;
    public List<AlumniRecord> getAll(String schoolId) { return repo.findBySchoolId(schoolId); }
    public AlumniRecord getOne(String schoolId, String id) { return repo.findById(id).filter(a -> a.getSchoolId().equals(schoolId)).orElseThrow(); }
    public AlumniRecord create(String schoolId, AlumniRecord record) { record.setSchoolId(schoolId); if (record.getStatus() == null) record.setStatus("ACTIVE"); return repo.save(record); }
    public AlumniRecord update(String schoolId, String id, AlumniRecord updated) {
        AlumniRecord r = getOne(schoolId, id);
        r.setFirstName(updated.getFirstName()); r.setLastName(updated.getLastName()); r.setAdmissionNumber(updated.getAdmissionNumber());
        r.setGraduationYear(updated.getGraduationYear()); r.setLastGrade(updated.getLastGrade());
        r.setCurrentEmployer(updated.getCurrentEmployer()); r.setCurrentPosition(updated.getCurrentPosition());
        r.setEmail(updated.getEmail()); r.setPhone(updated.getPhone()); r.setLocation(updated.getLocation()); r.setStatus(updated.getStatus());
        r.setIndustrySector(updated.getIndustrySector()); r.setHighestQualification(updated.getHighestQualification());
        r.setQualificationsAchieved(updated.getQualificationsAchieved()); r.setLinkedIn(updated.getLinkedIn());
        r.setEngagementStatus(updated.getEngagementStatus());
        return repo.save(r);
    }
    public void delete(String schoolId, String id) { repo.findById(id).filter(a -> a.getSchoolId().equals(schoolId)).ifPresent(repo::delete); }
}
