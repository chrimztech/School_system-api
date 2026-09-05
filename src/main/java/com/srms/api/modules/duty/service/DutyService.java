package com.srms.api.modules.duty.service;

import com.srms.api.modules.duty.entity.DutyAssignment;
import com.srms.api.modules.duty.repository.DutyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class DutyService {
    private final DutyRepository repo;
    public List<DutyAssignment> list(String schoolId) { return repo.findBySchoolIdOrderByDayOfWeekAsc(schoolId); }
    public DutyAssignment create(String schoolId, DutyAssignment d) { d.setSchoolId(schoolId); return repo.save(d); }
    public DutyAssignment update(String schoolId, String id, DutyAssignment updated) {
        DutyAssignment d = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        d.setStaffName(updated.getStaffName());
        d.setRole(updated.getRole());
        d.setDayOfWeek(updated.getDayOfWeek());
        d.setLocation(updated.getLocation());
        d.setStartTime(updated.getStartTime());
        d.setEndTime(updated.getEndTime());
        d.setWeek(updated.getWeek());
        d.setTerm(updated.getTerm());
        d.setBackupStaff(updated.getBackupStaff());
        d.setEffectiveDate(updated.getEffectiveDate());
        d.setRotationCycle(updated.getRotationCycle());
        d.setNotes(updated.getNotes());
        d.setApprovalStatus(updated.getApprovalStatus());
        return repo.save(d);
    }
    public void delete(String schoolId, String id) { repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).ifPresent(repo::delete); }
}
