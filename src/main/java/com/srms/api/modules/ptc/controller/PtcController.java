package com.srms.api.modules.ptc.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.ptc.entity.PtcMeeting;
import com.srms.api.modules.ptc.entity.PtcMember;
import com.srms.api.modules.ptc.entity.PtcTransaction;
import com.srms.api.modules.ptc.service.PtcService;
import com.srms.api.security.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/schools/{schoolId}/ptc")
@RequiredArgsConstructor
public class PtcController {
    private final PtcService ptcService;
    private final ModuleAccessService moduleAccessService;

    /** Roles that can view/manage PTC committee finances — everyone else (including parents) is excluded. */
    private static final Set<String> CAN_MANAGE_BUDGET_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "FINANCE");

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    private void requireCanManageBudget(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "ptc-budget", "full", CAN_MANAGE_BUDGET_ROLES.contains(roleOf(auth)))) {
            throw new ForbiddenException("Your role does not have permission to view or manage PTC committee finances");
        }
    }

    // Members
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<PtcMember>>> listMembers(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(ptcService.listMembers(schoolId)));
    }

    @PostMapping("/members")
    public ResponseEntity<ApiResponse<PtcMember>> createMember(@PathVariable String schoolId, @RequestBody PtcMember m) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(ptcService.createMember(schoolId, m)));
    }

    @PutMapping("/members/{id}")
    public ResponseEntity<ApiResponse<PtcMember>> updateMember(@PathVariable String schoolId, @PathVariable String id, @RequestBody PtcMember m) {
        return ResponseEntity.ok(ApiResponse.ok(ptcService.updateMember(schoolId, id, m)));
    }

    @DeleteMapping("/members/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable String schoolId, @PathVariable String id) {
        ptcService.deleteMember(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted", null));
    }

    // Meetings
    @GetMapping("/meetings")
    public ResponseEntity<ApiResponse<List<PtcMeeting>>> listMeetings(@PathVariable String schoolId, @RequestParam(defaultValue = "false") boolean publishedOnly) {
        return ResponseEntity.ok(ApiResponse.ok(ptcService.listMeetings(schoolId, publishedOnly)));
    }

    @PostMapping("/meetings")
    public ResponseEntity<ApiResponse<PtcMeeting>> createMeeting(@PathVariable String schoolId, @RequestBody PtcMeeting meeting) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(ptcService.createMeeting(schoolId, meeting)));
    }

    @PatchMapping("/meetings/{id}")
    public ResponseEntity<ApiResponse<PtcMeeting>> updateMeeting(@PathVariable String schoolId, @PathVariable String id, @RequestBody PtcMeeting patch) {
        return ResponseEntity.ok(ApiResponse.ok(ptcService.updateMeeting(schoolId, id, patch)));
    }

    @PatchMapping("/meetings/{id}/publish")
    public ResponseEntity<ApiResponse<PtcMeeting>> publishMeeting(@PathVariable String schoolId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(ptcService.publishMeeting(schoolId, id)));
    }

    // Transactions
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<PtcTransaction>>> listTransactions(@PathVariable String schoolId, Authentication auth) {
        requireCanManageBudget(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(ptcService.listTransactions(schoolId)));
    }

    @PostMapping("/transactions")
    public ResponseEntity<ApiResponse<PtcTransaction>> createTransaction(@PathVariable String schoolId, @RequestBody PtcTransaction t, Authentication auth) {
        requireCanManageBudget(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(ptcService.createTransaction(schoolId, t)));
    }

    @PatchMapping("/transactions/{id}")
    public ResponseEntity<ApiResponse<PtcTransaction>> updateTransaction(@PathVariable String schoolId, @PathVariable String id, @RequestBody PtcTransaction patch, Authentication auth) {
        requireCanManageBudget(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(ptcService.updateTransaction(schoolId, id, patch)));
    }
}
