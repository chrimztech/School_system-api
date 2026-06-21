package com.srms.api.modules.duty.repository;

import com.srms.api.modules.duty.entity.DutyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DutyRepository extends JpaRepository<DutyAssignment, String> {
    List<DutyAssignment> findBySchoolIdOrderByDayOfWeekAsc(String schoolId);
}
