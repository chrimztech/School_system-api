package com.srms.api.modules.student.repository;

import com.srms.api.modules.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    List<Student> findBySchoolId(String schoolId);
    Page<Student> findBySchoolId(String schoolId, Pageable pageable);
    Optional<Student> findByIdAndSchoolId(String id, String schoolId);
    List<Student> findBySchoolIdAndStatus(String schoolId, Student.StudentStatus status);
    List<Student> findBySchoolIdAndGrade(String schoolId, int grade);
    List<Student> findBySchoolIdAndGuardianEmailIgnoreCase(String schoolId, String guardianEmail);
    @Query("SELECT COUNT(s) FROM Student s WHERE s.schoolId = :schoolId AND s.status = 'active'")
    long countActiveBySchoolId(String schoolId);

    // Dashboard aggregates — let Postgres do the sum/count instead of pulling every active
    // student row into the JVM to loop over (the previous approach: findBySchoolIdAndStatus
    // then Java-side stream().sum(), which transfers full row data for a school's entire
    // roster on every single dashboard load just to compute two numbers).
    @Query("SELECT COALESCE(SUM(s.feeBalance), 0) FROM Student s WHERE s.schoolId = :schoolId AND s.status = :status")
    double sumFeeBalanceBySchoolIdAndStatus(String schoolId, Student.StudentStatus status);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.schoolId = :schoolId AND s.status = :status AND s.grade <= :maxGrade")
    long countBySchoolIdAndStatusAndGradeLessThanEqual(String schoolId, Student.StudentStatus status, int maxGrade);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.schoolId = :schoolId AND s.admissionNumber LIKE :prefix%")
    long countBySchoolIdAndAdmissionNumberPrefix(String schoolId, String prefix);

    boolean existsByAdmissionNumber(String admissionNumber);
    List<Student> findBySchoolIdAndIdIn(String schoolId, List<String> ids);
}
