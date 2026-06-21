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

    @Query("SELECT COUNT(s) FROM Student s WHERE s.schoolId = :schoolId AND s.admissionNumber LIKE :prefix%")
    long countBySchoolIdAndAdmissionNumberPrefix(String schoolId, String prefix);

    boolean existsByAdmissionNumber(String admissionNumber);
}
