package com.srms.api.modules.academic.repository;

import com.srms.api.modules.academic.entity.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, String> {
    List<AcademicTerm> findBySchoolIdOrderByAcademicYearDescTermAsc(String schoolId);
    List<AcademicTerm> findBySchoolIdAndAcademicYearOrderByTermAsc(String schoolId, int academicYear);
    Optional<AcademicTerm> findBySchoolIdAndAcademicYearAndTerm(String schoolId, int academicYear, int term);
}
