package com.srms.api.modules.dashboard.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.attendance.dto.AttendanceSummary;
import com.srms.api.modules.attendance.service.AttendanceService;
import com.srms.api.modules.dashboard.dto.DashboardStats;
import com.srms.api.modules.fee.service.FeeService;
import com.srms.api.modules.school.dto.SchoolDto;
import com.srms.api.modules.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final SchoolService schoolService;
    private final AttendanceService attendanceService;
    private final FeeService feeService;

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
                        .outstanding(0)
                        .collectionRate(0)
                        .currency("ZMW").build())
                .enrolmentByPhase(List.of())
                .feeTrend(List.of())
                .attendanceTrend(List.of())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
