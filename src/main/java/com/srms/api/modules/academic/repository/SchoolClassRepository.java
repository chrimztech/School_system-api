package com.srms.api.modules.academic.repository;
import com.srms.api.modules.academic.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, String> {
    List<SchoolClass> findBySchoolIdAndActiveTrue(String schoolId);
    long countBySchoolIdAndActiveTrue(String schoolId);
    Optional<SchoolClass> findByIdAndSchoolId(String id, String schoolId);
    List<SchoolClass> findBySchoolIdAndGrade(String schoolId, int grade);
    Optional<SchoolClass> findBySchoolIdAndName(String schoolId, String name);
}
