package com.srms.api.modules.academic.repository;

import com.srms.api.modules.academic.entity.ClassEnrolment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClassEnrolmentRepository extends JpaRepository<ClassEnrolment, String> {
    List<ClassEnrolment> findByClassIdAndSchoolId(String classId, String schoolId);
    List<ClassEnrolment> findByStudentIdAndSchoolId(String studentId, String schoolId);
    Optional<ClassEnrolment> findByClassIdAndStudentIdAndAcademicYear(String classId, String studentId, String academicYear);
    boolean existsByClassIdAndStudentIdAndAcademicYear(String classId, String studentId, String academicYear);
}
