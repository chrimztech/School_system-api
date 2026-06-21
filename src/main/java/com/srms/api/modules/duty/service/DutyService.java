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
    public void delete(String schoolId, String id) { repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).ifPresent(repo::delete); }
}
