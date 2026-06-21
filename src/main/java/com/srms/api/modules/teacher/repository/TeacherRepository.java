package com.srms.api.modules.teacher.repository;
import com.srms.api.modules.teacher.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, String> {
    List<Teacher> findBySchoolId(String schoolId);
    Optional<Teacher> findByIdAndSchoolId(String id, String schoolId);
    List<Teacher> findBySchoolIdAndStatus(String schoolId, Teacher.TeacherStatus status);
    long countBySchoolIdAndStatus(String schoolId, Teacher.TeacherStatus status);
}
