package com.srms.api.modules.communication.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.communication.dto.AnnouncementDto;
import com.srms.api.modules.communication.dto.MessageDto;
import com.srms.api.modules.communication.entity.Announcement;
import com.srms.api.modules.communication.entity.Message;
import com.srms.api.modules.communication.service.CommunicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}")
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationService communicationService;

    // ── Announcements ──────────────────────────────────────────────────────────

    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<Announcement>>> getAnnouncements(
            @PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(communicationService.getAnnouncements(schoolId)));
    }

    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<Announcement>> createAnnouncement(
            @PathVariable String schoolId,
            @RequestBody AnnouncementDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(communicationService.createAnnouncement(schoolId, dto)));
    }

    @PutMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Announcement>> updateAnnouncement(
            @PathVariable String schoolId,
            @PathVariable String id,
            @RequestBody AnnouncementDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(communicationService.updateAnnouncement(schoolId, id, dto)));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable String schoolId,
            @PathVariable String id) {
        communicationService.deleteAnnouncement(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Announcement deleted", null));
    }

    // ── Messages ───────────────────────────────────────────────────────────────

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getMessages(
            @PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(communicationService.getMessages(schoolId)));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @PathVariable String schoolId,
            @RequestBody MessageDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(communicationService.sendMessage(schoolId, dto)));
    }

    @PutMapping("/messages/{id}/reply")
    public ResponseEntity<ApiResponse<Message>> replyToMessage(
            @PathVariable String schoolId,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String replyBody = body.get("replyBody");
        return ResponseEntity.ok(ApiResponse.ok(communicationService.replyToMessage(schoolId, id, replyBody)));
    }
}
