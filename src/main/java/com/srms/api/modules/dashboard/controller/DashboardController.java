package com.srms.api.modules.dashboard.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.attendance.dto.AttendanceSummary;
import com.srms.api.modules.attendance.service.AttendanceService;
import com.srms.api.modules.dashboard.dto.DashboardStats;
import com.srms.api.modules.fee.service.FeeService;
import com.srms.api.modules.school.dto.SchoolDto;
import com.srms.api.modules.school.service.SchoolService;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final SchoolService schoolService;
    private final AttendanceService attendanceService;
    private final FeeService feeService;
    private final StudentRepository studentRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardStats>> getStats(@PathVariable String schoolId, org.springframework.security.core.Authentication auth) {
        // Prevent platform (SUPER_ADMIN) users from accidentally using the school dashboard endpoint.
        boolean isSuper = auth.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (isSuper) {
            return ResponseEntity.status(403).body(ApiResponse.error("Use /api/platform/dashboard for system-level metrics"));
        }

        SchoolDto school = schoolService.findById(schoolId);
        AttendanceSummary attendance = attendanceService.getTodaySummary(schoolId);
        double feesCollected = feeService.getTotalCollected(schoolId);

        List<Student> activeStudents = studentRepository.findBySchoolIdAndStatus(schoolId, Student.StudentStatus.active);
        double outstanding = activeStudents.stream().mapToDouble(Student::getFeeBalance).sum();
        double totalBilled = feesCollected + outstanding;
        double collectionRate = totalBilled > 0 ? (feesCollected / totalBilled) * 100 : 0;

        String type = school.getType() == null ? "" : school.getType().toUpperCase();
        boolean combined = type.equals("COMBINED") || type.equals("FULL");
        Map<String, Integer> phaseCounts = new LinkedHashMap<>();
        phaseCounts.put("Primary", 0);
        phaseCounts.put("Secondary", 0);
        for (Student s : activeStudents) {
            boolean isPrimary = combined ? s.getGrade() <= 6 : type.equals("PRIMARY") || type.equals("NURSERY");
            phaseCounts.merge(isPrimary ? "Primary" : "Secondary", 1, Integer::sum);
        }
        List<DashboardStats.PhaseEnrolment> enrolmentByPhase = phaseCounts.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> DashboardStats.PhaseEnrolment.builder().phase(e.getKey()).count(e.getValue()).build())
                .toList();

        DashboardStats stats = DashboardStats.builder()
                .totalStudents(school.getTotalStudents())
                .totalTeachers(school.getTotalTeachers())
                .totalClasses(school.getTotalClasses())
                .attendanceToday(DashboardStats.AttendanceStats.builder()
                        .present(attendance.getPresent())
                        .absent(attendance.getAbsent())
                        .late(attendance.getLate())
                        .rate(attendance.getRate()).build())
                .fees(DashboardStats.FeeStats.builder()
                        .collected((long) feesCollected)
                        .outstanding((long) outstanding)
                        .collectionRate(Math.round(collectionRate * 10.0) / 10.0)
                        .currency("ZMW").build())
                .enrolmentByPhase(enrolmentByPhase)
                .feeTrend(List.of())
                .attendanceTrend(List.of())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
