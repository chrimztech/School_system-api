package com.srms.api.modules.dashboard.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.school.dto.SchoolDto;
import com.srms.api.modules.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/platform/dashboard")
@RequiredArgsConstructor
public class PlatformDashboardController {
    private final SchoolService schoolService;

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getPlatformStats(org.springframework.security.core.Authentication auth) {
        boolean isSuper = auth.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (!isSuper) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only system administrators can access platform metrics"));
        }

        List<SchoolDto> schools = schoolService.findAll();
        long totalSchools = schools.size();
        long totalStudents = schools.stream().mapToLong(s -> s.getTotalStudents() == null ? 0 : s.getTotalStudents()).sum();
        long totalTeachers = schools.stream().mapToLong(s -> s.getTotalTeachers() == null ? 0 : s.getTotalTeachers()).sum();
        long totalClasses = schools.stream().mapToLong(s -> s.getTotalClasses() == null ? 0 : s.getTotalClasses()).sum();

        var body = java.util.Map.of(
                "totalSchools", totalSchools,
                "totalStudents", totalStudents,
                "totalTeachers", totalTeachers,
                "totalClasses", totalClasses,
                "schools", schools
        );
        return ResponseEntity.ok(ApiResponse.ok(body));
    }
}
