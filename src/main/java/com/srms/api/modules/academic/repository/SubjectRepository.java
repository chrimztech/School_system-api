package com.srms.api.modules.academic.repository;
import com.srms.api.modules.academic.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SubjectRepository extends JpaRepository<Subject, String> {
    List<Subject> findBySchoolIdAndActiveTrue(String schoolId);
    Optional<Subject> findByIdAndSchoolId(String id, String schoolId);
    boolean existsBySchoolIdAndCodeAndPhase(String schoolId, String code, String phase);
}
