package com.srms.api.modules.attendance.repository;
import com.srms.api.modules.attendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, String> {
    List<AttendanceRecord> findBySchoolIdAndDate(String schoolId, LocalDate date);
    List<AttendanceRecord> findBySchoolIdAndStudentIdAndDate(String schoolId, String studentId, LocalDate date);
    List<AttendanceRecord> findBySchoolIdAndStudentIdOrderByDateDesc(String schoolId, String studentId);
    List<AttendanceRecord> findBySchoolIdAndClassIdAndDate(String schoolId, String classId, LocalDate date);
    List<AttendanceRecord> findBySchoolIdAndDateBetween(String schoolId, LocalDate from, LocalDate to);
    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.schoolId=:schoolId AND a.date=:date AND a.status='present'")
    long countPresentBySchoolAndDate(String schoolId, LocalDate date);
    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.schoolId=:schoolId AND a.date=:date")
    long countTotalBySchoolAndDate(String schoolId, LocalDate date);
}
