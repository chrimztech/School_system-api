package com.srms.api.modules.timetable.repository;

import com.srms.api.modules.timetable.entity.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TimetableRepository extends JpaRepository<TimetableSlot, String> {
    List<TimetableSlot> findBySchoolId(String schoolId);
    List<TimetableSlot> findBySchoolIdAndClassId(String schoolId, String classId);
    List<TimetableSlot> findBySchoolIdAndTeacherId(String schoolId, String teacherId);
    void deleteBySchoolIdAndClassId(String schoolId, String classId);
}
