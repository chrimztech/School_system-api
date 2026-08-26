package com.srms.api.modules.communication.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.communication.dto.AnnouncementDto;
import com.srms.api.modules.communication.dto.MessageDto;
import com.srms.api.modules.communication.entity.Announcement;
import com.srms.api.modules.communication.entity.Message;
import com.srms.api.modules.communication.service.CommunicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/schools/{schoolId}")
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationService communicationService;

    /** Mirrors the frontend's SCHOOL_LEADERSHIP_ROLES — the only roles allowed to mass-broadcast
     *  or manage announcements, so a parent/teacher/HOD/finance/career-guidance account can't
     *  reach the same result by calling the API directly once the UI button is hidden from them. */
    private static final Set<String> ANNOUNCEMENT_MANAGER_ROLES =
            Set.of("SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    private static void requireAnnouncementManager(Authentication auth) {
        if (!ANNOUNCEMENT_MANAGER_ROLES.contains(roleOf(auth))) {
            throw new ForbiddenException("Only school leadership can manage announcements");
        }
    }

    // ── Announcements ──────────────────────────────────────────────────────────

    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<Announcement>>> getAnnouncements(
            @PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(communicationService.getAnnouncements(schoolId)));
    }

    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<Announcement>> createAnnouncement(
            @PathVariable String schoolId,
            @RequestBody AnnouncementDto dto,
            Authentication auth) {
        requireAnnouncementManager(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(communicationService.createAnnouncement(schoolId, dto)));
    }

    @PutMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Announcement>> updateAnnouncement(
            @PathVariable String schoolId,
            @PathVariable String id,
            @RequestBody AnnouncementDto dto,
            Authentication auth) {
        requireAnnouncementManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(communicationService.updateAnnouncement(schoolId, id, dto)));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable String schoolId,
            @PathVariable String id,
            Authentication auth) {
        requireAnnouncementManager(auth);
        communicationService.deleteAnnouncement(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Announcement deleted", null));
    }

    /**
     * Remaining Zamtel SMS credit. Super-admin only — the API key/credit pool is one platform-
     * wide account shared across every school, not a per-school resource, so surfacing it to a
     * single school's admins would misleadingly read as "your school's balance" and give them
     * no way to act on it anyway.
     */
    @GetMapping("/sms-balance")
    public ResponseEntity<ApiResponse<Long>> getSmsBalance(@PathVariable String schoolId, Authentication auth) {
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SUPER_ADMIN"::equals);
        if (!isSuperAdmin) throw new ForbiddenException("Only the platform administrator can view the SMS balance");
        return ResponseEntity.ok(ApiResponse.ok(communicationService.getSmsBalance()));
    }

    // ── Messages ───────────────────────────────────────────────────────────────

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<?>> getMessages(
            @PathVariable String schoolId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortDir) {
        Pageable pageable = PageRequestUtil.build(page, size, sortBy, sortDir);
        if (pageable == null) return ResponseEntity.ok(ApiResponse.ok(communicationService.getMessages(schoolId)));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(communicationService.getMessagesPaged(schoolId, pageable))));
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

    @PutMapping("/messages/{id}/close")
    public ResponseEntity<ApiResponse<Message>> closeMessage(
            @PathVariable String schoolId,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(communicationService.closeMessage(schoolId, id)));
    }
}
