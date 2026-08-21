package com.srms.api.modules.attendance.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.academic.entity.SchoolClass;
import com.srms.api.modules.academic.service.AcademicService;
import com.srms.api.modules.attendance.dto.AttendanceDto;
import com.srms.api.modules.attendance.dto.AttendanceSummary;
import com.srms.api.modules.attendance.entity.AttendanceRecord;
import com.srms.api.modules.attendance.service.AttendanceService;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.student.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@RestController @RequestMapping("/api/schools/{schoolId}/attendance") @RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final AcademicService academicService;
    private final UserRepository userRepository;

    /** Roles with "full" (not "read") access to the attendance module. */
    private static final Set<String> CAN_MARK_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "TEACHER", "PRINCIPAL", "DEPUTY_HEAD");

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    /**
     * Registers are per-class — a teacher must only ever see attendance for their own
     * classes, derived from their authenticated identity, never a client-supplied filter.
     * Returns null for non-teacher callers (leadership sees everything, unfiltered).
     */
    private List<AttendanceRecord> scopeToTeacher(String schoolId, Authentication auth, List<AttendanceRecord> records) {
        if (!"TEACHER".equals(roleOf(auth))) return records;
        String email = userRepository.findById(auth.getName()).map(AppUser::getEmail).orElse(null);
        if (email == null) return List.of();
        // AttendanceRecord.classId is populated from the register form's class selector, which
        // identifies a class by its display name (see the matching note on mark() below) — a
        // record's classId is therefore usually a name, occasionally a real id. Match both.
        Set<String> myClassKeys = new HashSet<>();
        for (SchoolClass c : academicService.findClassesByTeacherEmail(schoolId, email)) {
            if (c.getName() != null) myClassKeys.add(c.getName());
            if (c.getId() != null) myClassKeys.add(c.getId());
        }
        return records.stream().filter(r -> myClassKeys.contains(r.getClassId())).toList();
    }

    @GetMapping public ResponseEntity<ApiResponse<List<AttendanceRecord>>> getToday(@PathVariable String schoolId, Authentication auth) { return ResponseEntity.ok(ApiResponse.ok(scopeToTeacher(schoolId, auth, attendanceService.getTodayAttendance(schoolId)))); }
    @GetMapping("/summary") public ResponseEntity<ApiResponse<AttendanceSummary>> getSummary(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(attendanceService.getTodaySummary(schoolId))); }
    @GetMapping("/date/{date}") public ResponseEntity<ApiResponse<List<AttendanceRecord>>> getByDate(@PathVariable String schoolId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Authentication auth) { return ResponseEntity.ok(ApiResponse.ok(scopeToTeacher(schoolId, auth, attendanceService.getByDate(schoolId, date)))); }
    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<?>> getStudentAttendance(
            @PathVariable String schoolId, @PathVariable String studentId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortDir,
            Authentication auth) {
        if ("TEACHER".equals(roleOf(auth))) {
            String email = userRepository.findById(auth.getName()).map(AppUser::getEmail).orElse(null);
            boolean isMyStudent = email != null && academicService.findStudentsByTeacherEmail(schoolId, email)
                    .stream().map(Student::getId).anyMatch(studentId::equals);
            if (!isMyStudent) throw new ForbiddenException("This student is not in one of your classes");
        }
        Pageable pageable = PageRequestUtil.build(page, size, sortBy, sortDir);
        if (pageable == null) return ResponseEntity.ok(ApiResponse.ok(attendanceService.getStudentAttendance(schoolId, studentId)));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(attendanceService.getStudentAttendancePaged(schoolId, studentId, pageable))));
    }
    @PostMapping public ResponseEntity<ApiResponse<List<AttendanceRecord>>> mark(@PathVariable String schoolId, @RequestBody AttendanceDto dto, Authentication auth) {
        String role = roleOf(auth);
        if (!CAN_MARK_ROLES.contains(role)) {
            throw new ForbiddenException("Your role does not have permission to mark attendance");
        }
        if ("TEACHER".equals(role) && dto.getClassId() != null) {
            // The frontend register selector identifies a class by its display name (falling
            // back to id only when a class has no name) rather than its real id — match the
            // same way here, or a legitimate submission would be rejected as "not your class".
            String email = userRepository.findById(auth.getName()).map(AppUser::getEmail).orElse(null);
            boolean isMyClass = email != null && academicService.findClassesByTeacherEmail(schoolId, email).stream()
                    .anyMatch(c -> dto.getClassId().equals(c.getName()) || dto.getClassId().equals(c.getId()));
            if (!isMyClass) throw new ForbiddenException("You are not assigned to this class");
        }
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.markAttendance(schoolId, dto)));
    }
}
