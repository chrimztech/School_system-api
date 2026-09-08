package com.srms.api.modules.platform.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.BusinessException;
import com.srms.api.modules.platform.dto.SupportTicketRequest;
import com.srms.api.modules.platform.service.PlatformWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Book a demo" on the public marketing landing page — unauthenticated, mounted under
 * /api/public/** (permitAll in SecurityConfig), since a prospective school has no account yet.
 * Lands in the exact same Support Desk queue as SupportRequestController's authenticated
 * tickets (same underlying service call), just tagged with a distinct category so platform
 * staff can tell a sales lead apart from an existing school's support issue at a glance.
 */
@RestController
@RequestMapping("/api/public/demo-requests")
@RequiredArgsConstructor
public class PublicDemoRequestController {
    private final PlatformWorkspaceService platformWorkspaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(@RequestBody SupportTicketRequest request) {
        if (isBlank(request.getReporterName())) {
            throw new BusinessException("Your name is required");
        }
        if (isBlank(request.getTenantName())) {
            throw new BusinessException("School / organisation name is required");
        }
        if (isBlank(request.getReporterEmail())) {
            throw new BusinessException("An email or phone number is required so we can reach you");
        }
        request.setCategory("Demo request");
        if (isBlank(request.getSubject())) {
            request.setSubject("Demo request from " + request.getTenantName());
        }
        platformWorkspaceService.submitSupportTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Request submitted", null));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
