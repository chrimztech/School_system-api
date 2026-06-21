package com.srms.api.modules.timetable.service;

import com.srms.api.modules.timetable.entity.TimetableSlot;
import com.srms.api.modules.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class TimetableService {
    private final TimetableRepository repo;
    public List<TimetableSlot> getAll(String schoolId) { return repo.findBySchoolId(schoolId); }
    public List<TimetableSlot> getByClass(String schoolId, String classId) { return repo.findBySchoolIdAndClassId(schoolId, classId); }
    public List<TimetableSlot> getByTeacher(String schoolId, String teacherId) { return repo.findBySchoolIdAndTeacherId(schoolId, teacherId); }
    public TimetableSlot create(String schoolId, TimetableSlot slot) { slot.setSchoolId(schoolId); return repo.save(slot); }
    public TimetableSlot update(String schoolId, String id, TimetableSlot updated) {
        TimetableSlot slot = repo.findById(id).filter(s -> s.getSchoolId().equals(schoolId)).orElseThrow();
        slot.setDayOfWeek(updated.getDayOfWeek()); slot.setStartTime(updated.getStartTime()); slot.setEndTime(updated.getEndTime()); slot.setPeriod(updated.getPeriod()); slot.setSubjectId(updated.getSubjectId()); slot.setSubjectName(updated.getSubjectName()); slot.setTeacherId(updated.getTeacherId()); slot.setTeacherName(updated.getTeacherName()); slot.setRoom(updated.getRoom());
        return repo.save(slot);
    }
    public void delete(String schoolId, String id) { repo.findById(id).filter(s -> s.getSchoolId().equals(schoolId)).ifPresent(repo::delete); }
}
