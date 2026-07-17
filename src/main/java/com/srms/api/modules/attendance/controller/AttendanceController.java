package com.srms.api.modules.attendance.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.modules.attendance.dto.AttendanceDto;
import com.srms.api.modules.attendance.dto.AttendanceSummary;
import com.srms.api.modules.attendance.entity.AttendanceRecord;
import com.srms.api.modules.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/schools/{schoolId}/attendance") @RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;
    @GetMapping public ResponseEntity<ApiResponse<List<AttendanceRecord>>> getToday(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(attendanceService.getTodayAttendance(schoolId))); }
    @GetMapping("/summary") public ResponseEntity<ApiResponse<AttendanceSummary>> getSummary(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(attendanceService.getTodaySummary(schoolId))); }
    @GetMapping("/date/{date}") public ResponseEntity<ApiResponse<List<AttendanceRecord>>> getByDate(@PathVariable String schoolId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { return ResponseEntity.ok(ApiResponse.ok(attendanceService.getByDate(schoolId, date))); }
    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<?>> getStudentAttendance(
            @PathVariable String schoolId, @PathVariable String studentId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortDir) {
        Pageable pageable = PageRequestUtil.build(page, size, sortBy, sortDir);
        if (pageable == null) return ResponseEntity.ok(ApiResponse.ok(attendanceService.getStudentAttendance(schoolId, studentId)));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(attendanceService.getStudentAttendancePaged(schoolId, studentId, pageable))));
    }
    @PostMapping public ResponseEntity<ApiResponse<List<AttendanceRecord>>> mark(@PathVariable String schoolId, @RequestBody AttendanceDto dto) { return ResponseEntity.ok(ApiResponse.ok(attendanceService.markAttendance(schoolId, dto))); }
}
