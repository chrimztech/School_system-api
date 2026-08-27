package com.srms.api.modules.attendance.service;
import com.srms.api.modules.attendance.dto.AttendanceDto;
import com.srms.api.modules.attendance.dto.AttendanceSummary;
import com.srms.api.modules.attendance.entity.AttendanceRecord;
import com.srms.api.modules.attendance.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service @RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    public List<AttendanceRecord> getTodayAttendance(String schoolId) {
        return attendanceRepository.findBySchoolIdAndDate(schoolId, LocalDate.now());
    }
    public List<AttendanceRecord> getByDate(String schoolId, LocalDate date) {
        return attendanceRepository.findBySchoolIdAndDate(schoolId, date);
    }
    public List<AttendanceRecord> getStudentAttendance(String schoolId, String studentId) {
        return attendanceRepository.findBySchoolIdAndStudentIdOrderByDateDesc(schoolId, studentId);
    }
    public Page<AttendanceRecord> getStudentAttendancePaged(String schoolId, String studentId, Pageable pageable) {
        return attendanceRepository.findBySchoolIdAndStudentIdOrderByDateDesc(schoolId, studentId, pageable);
    }
    /**
     * Marking a class register used to issue one SELECT + one UPDATE/INSERT per student
     * (2 round trips × class size) — with every class in every school submitting a register
     * around the same time each morning, that's the single most concurrency-sensitive write
     * path in the system. Fetches every existing record for this school/date once, then
     * resolves the whole class in memory and saves in one batch (Hibernate's configured
     * jdbc.batch_size groups the actual INSERT/UPDATE statements).
     */
    public List<AttendanceRecord> markAttendance(String schoolId, AttendanceDto dto) {
        List<AttendanceRecord> existingForDate = attendanceRepository.findBySchoolIdAndDate(schoolId, dto.getDate());
        Map<String, List<AttendanceRecord>> byStudent = new HashMap<>();
        for (AttendanceRecord r : existingForDate) {
            byStudent.computeIfAbsent(r.getStudentId(), k -> new ArrayList<>()).add(r);
        }

        List<AttendanceRecord> toSave = new ArrayList<>();
        List<AttendanceRecord> toDelete = new ArrayList<>();
        for (AttendanceDto.AttendanceEntry entry : dto.getEntries()) {
            List<AttendanceRecord> existing = byStudent.get(entry.getStudentId());
            AttendanceRecord record;
            if (existing != null && !existing.isEmpty()) {
                record = existing.get(0);
                if (existing.size() > 1) toDelete.addAll(existing.subList(1, existing.size()));
            } else {
                record = AttendanceRecord.builder()
                        .schoolId(schoolId)
                        .studentId(entry.getStudentId())
                        .classId(dto.getClassId())
                        .date(dto.getDate())
                        .build();
            }
            record.setStudentName(entry.getStudentName());
            record.setClassName(dto.getClassName());
            record.setStatus(AttendanceRecord.AttendanceStatus.valueOf(entry.getStatus()));
            record.setRemarks(entry.getRemarks());
            toSave.add(record);
        }

        if (!toDelete.isEmpty()) attendanceRepository.deleteAll(toDelete);
        return attendanceRepository.saveAll(toSave);
    }
    public AttendanceSummary getTodaySummary(String schoolId) {
        LocalDate today = LocalDate.now();
        List<AttendanceRecord> records = attendanceRepository.findBySchoolIdAndDate(schoolId, today);
        long present = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.present).count();
        long absent = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.absent).count();
        long late = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.late).count();
        long excused = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.excused).count();
        long sick = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.sick).count();
        int total = records.size();
        double rate = total > 0 ? (double) present / total * 100 : 0;
        return AttendanceSummary.builder()
                .present((int)present).absent((int)absent).late((int)late)
                .excused((int)excused).sick((int)sick).total(total).rate(Math.round(rate * 10.0) / 10.0)
                .build();
    }
}
