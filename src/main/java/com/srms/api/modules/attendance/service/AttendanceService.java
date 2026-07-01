package com.srms.api.modules.attendance.service;
import com.srms.api.modules.attendance.dto.AttendanceDto;
import com.srms.api.modules.attendance.dto.AttendanceSummary;
import com.srms.api.modules.attendance.entity.AttendanceRecord;
import com.srms.api.modules.attendance.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
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
    public List<AttendanceRecord> markAttendance(String schoolId, AttendanceDto dto) {
        return dto.getEntries().stream().map(entry -> {
            List<AttendanceRecord> existing = attendanceRepository
                    .findBySchoolIdAndStudentIdAndDate(schoolId, entry.getStudentId(), dto.getDate());
            AttendanceRecord record;
            if (!existing.isEmpty()) {
                record = existing.get(0);
                if (existing.size() > 1) {
                    attendanceRepository.deleteAll(existing.subList(1, existing.size()));
                }
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
            return attendanceRepository.save(record);
        }).collect(Collectors.toList());
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
