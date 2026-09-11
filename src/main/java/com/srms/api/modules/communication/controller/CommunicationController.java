package com.srms.api.modules.communication.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.communication.dto.AnnouncementDto;
import com.srms.api.modules.communication.dto.MessageDto;
import com.srms.api.modules.communication.entity.Announcement;
import com.srms.api.modules.communication.entity.Message;
import com.srms.api.modules.communication.service.CommunicationService;
import com.srms.api.security.ModuleAccessService;
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
    private final ModuleAccessService moduleAccessService;
    private final UserRepository userRepository;

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

    private void requireAnnouncementManager(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "communication", "full", ANNOUNCEMENT_MANAGER_ROLES.contains(roleOf(auth)))) {
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
        requireAnnouncementManager(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(communicationService.createAnnouncement(schoolId, dto)));
    }

    @PutMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Announcement>> updateAnnouncement(
            @PathVariable String schoolId,
            @PathVariable String id,
            @RequestBody AnnouncementDto dto,
            Authentication auth) {
        requireAnnouncementManager(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(communicationService.updateAnnouncement(schoolId, id, dto)));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable String schoolId,
            @PathVariable String id,
            Authentication auth) {
        requireAnnouncementManager(schoolId, auth);
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
    // This is a shared parent<->school ticket inbox (Message has senderEmail/recipientEmail,
    // not a broadcast) — getMessages returned every family's private correspondence to any
    // authenticated caller with no filtering at all. A parent may only ever see and send their
    // own messages; replying/closing is the school's side of the conversation, so those two
    // actions are staff-only regardless of whose thread it is.

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<?>> getMessages(
            @PathVariable String schoolId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortDir,
            Authentication auth) {
        if ("PARENT".equals(roleOf(auth))) {
            String email = currentEmail(auth);
            // A parent's own thread includes messages they raised (senderEmail) AND messages
            // staff initiated to them directly (recipientEmail) — without the latter, staff
            // messaging a parent first (rather than replying to an existing ticket) would be
            // invisible to that parent forever.
            List<Message> mine = communicationService.getMessages(schoolId).stream()
                    .filter(m -> email != null && (email.equalsIgnoreCase(m.getSenderEmail()) || email.equalsIgnoreCase(m.getRecipientEmail())))
                    .toList();
            return ResponseEntity.ok(ApiResponse.ok(mine));
        }
        Pageable pageable = PageRequestUtil.build(page, size, sortBy, sortDir);
        if (pageable == null) return ResponseEntity.ok(ApiResponse.ok(communicationService.getMessages(schoolId)));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(communicationService.getMessagesPaged(schoolId, pageable))));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @PathVariable String schoolId,
            @RequestBody MessageDto dto,
            Authentication auth) {
        if ("PARENT".equals(roleOf(auth))) {
            // Never trust a client-supplied sender identity — otherwise a parent could send a
            // message that reads as if it came from a different family.
            AppUser user = userRepository.findById(auth.getName())
                    .orElseThrow(() -> new ForbiddenException("Authenticated parent was not found"));
            dto.setSenderEmail(user.getEmail());
            dto.setSenderName(user.getName());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(communicationService.sendMessage(schoolId, dto)));
    }

    @PutMapping("/messages/{id}/reply")
    public ResponseEntity<ApiResponse<Message>> replyToMessage(
            @PathVariable String schoolId,
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        requireStaff(auth, "reply to");
        String replyBody = body.get("replyBody");
        return ResponseEntity.ok(ApiResponse.ok(communicationService.replyToMessage(schoolId, id, replyBody)));
    }

    @PutMapping("/messages/{id}/close")
    public ResponseEntity<ApiResponse<Message>> closeMessage(
            @PathVariable String schoolId,
            @PathVariable String id,
            Authentication auth) {
        requireStaff(auth, "close");
        return ResponseEntity.ok(ApiResponse.ok(communicationService.closeMessage(schoolId, id)));
    }

    private void requireStaff(Authentication auth, String action) {
        if ("PARENT".equals(roleOf(auth))) {
            throw new ForbiddenException("Only school staff can " + action + " a message");
        }
    }

    private String currentEmail(Authentication auth) {
        return userRepository.findById(auth.getName()).map(AppUser::getEmail).orElse(null);
    }
}
