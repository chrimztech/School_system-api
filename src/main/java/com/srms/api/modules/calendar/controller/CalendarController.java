package com.srms.api.modules.calendar.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.calendar.entity.CalendarEvent;
import com.srms.api.modules.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/calendar") @RequiredArgsConstructor
public class CalendarController {
    private final CalendarService calendarService;
    @GetMapping public ResponseEntity<ApiResponse<List<CalendarEvent>>> list(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(calendarService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<CalendarEvent>> create(@PathVariable String schoolId, @RequestBody CalendarEvent e) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(calendarService.create(schoolId, e))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) { calendarService.delete(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Deleted", null)); }
}
