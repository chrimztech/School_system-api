package com.srms.api.modules.alumni.repository;

import com.srms.api.modules.alumni.entity.AlumniRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlumniRepository extends JpaRepository<AlumniRecord, String> {
    List<AlumniRecord> findBySchoolId(String schoolId);
    List<AlumniRecord> findBySchoolIdAndGraduationYear(String schoolId, int year);
}
