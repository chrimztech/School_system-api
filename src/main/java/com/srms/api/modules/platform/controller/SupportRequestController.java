package com.srms.api.modules.platform.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.platform.dto.SupportTicketRequest;
import com.srms.api.modules.platform.service.PlatformWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets any authenticated school user (not just super admins) raise a support ticket that lands
 * in the platform Support Desk queue — deliberately separate from PlatformWorkspaceController,
 * which is super-admin-only and exposes the entire platform blob. This only ever appends one
 * ticket. */
@RestController
@RequestMapping("/api/support-requests")
@RequiredArgsConstructor
public class SupportRequestController {
    private final PlatformWorkspaceService platformWorkspaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(@RequestBody SupportTicketRequest request) {
        platformWorkspaceService.submitSupportTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Ticket submitted", null));
    }
}
