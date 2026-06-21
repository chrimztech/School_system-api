package com.srms.api.modules.academic.repository;

import com.srms.api.modules.academic.entity.TeacherClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeacherClassSubjectRepository extends JpaRepository<TeacherClassSubject, String> {
    List<TeacherClassSubject> findByClassIdAndSchoolId(String classId, String schoolId);
    List<TeacherClassSubject> findByTeacherIdAndSchoolId(String teacherId, String schoolId);
}
