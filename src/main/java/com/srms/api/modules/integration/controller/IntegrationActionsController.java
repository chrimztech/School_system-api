package com.srms.api.modules.integration.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.BusinessException;
import com.srms.api.modules.fee.repository.FeePaymentRepository;
import com.srms.api.modules.integration.client.AfricasTalkingSmsClient;
import com.srms.api.modules.integration.client.AirtelMoneyClient;
import com.srms.api.modules.integration.client.EczSyncClient;
import com.srms.api.modules.integration.client.GoogleWorkspaceClient;
import com.srms.api.modules.integration.client.IntegrationTestResult;
import com.srms.api.modules.integration.client.MtnMomoClient;
import com.srms.api.modules.integration.client.PowerBiClient;
import com.srms.api.modules.integration.client.ZoomClient;
import com.srms.api.modules.integration.dto.IntegrationConnectionView;
import com.srms.api.modules.integration.service.IntegrationService;
import com.srms.api.modules.student.repository.StudentRepository;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Real, live actions for the per-school integrations connected on the Integrations page —
 * separate from the CRUD in IntegrationController, which only ever stores credentials.
 * Everything here makes an actual outbound call to the provider using those credentials.
 */
@RestController
@RequestMapping("/api/schools/{schoolId}/integrations/{code}")
@RequiredArgsConstructor
public class IntegrationActionsController {
    private final IntegrationService integrationService;
    private final AfricasTalkingSmsClient africasTalkingSmsClient;
    private final MtnMomoClient mtnMomoClient;
    private final AirtelMoneyClient airtelMoneyClient;
    private final PowerBiClient powerBiClient;
    private final ZoomClient zoomClient;
    private final EczSyncClient eczSyncClient;
    private final GoogleWorkspaceClient googleWorkspaceClient;
    private final StudentRepository studentRepository;
    private final FeePaymentRepository feePaymentRepository;

    /** Runs a real, safe (non-money-moving, non-destructive) call against the provider using the
     * school's saved credentials, and records the outcome as the connection's live status. */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<IntegrationConnectionView>> test(
            @PathVariable String schoolId, @PathVariable String code, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        IntegrationTestResult result = switch (code) {
            case AfricasTalkingSmsClient.CODE -> africasTalkingSmsClient.test(schoolId);
            case MtnMomoClient.CODE -> mtnMomoClient.test(schoolId);
            case AirtelMoneyClient.CODE -> airtelMoneyClient.test(schoolId);
            case PowerBiClient.CODE -> powerBiClient.test(schoolId);
            case ZoomClient.CODE -> zoomClient.test(schoolId);
            case EczSyncClient.CODE -> eczSyncClient.test(schoolId);
            case GoogleWorkspaceClient.CODE -> googleWorkspaceClient.test(schoolId);
            default -> throw new BusinessException("No live test is available for \"" + code + "\" yet");
        };
        var updated = integrationService.recordTestOutcome(schoolId, code, result.success(), result.message());
        if (!result.success()) {
            throw new BusinessException(result.message());
        }
        return updated
                .map(c -> ResponseEntity.ok(ApiResponse.ok(IntegrationConnectionView.from(c))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null)));
    }

    /** Creates a real, scheduled Zoom meeting and returns its join URL. */
    @PostMapping("/zoom/create-meeting")
    public ResponseEntity<ApiResponse<Map<String, String>>> createZoomMeeting(
            @PathVariable String schoolId, @PathVariable String code, @RequestBody Map<String, String> body, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        String topic = body.getOrDefault("topic", "SRMS meeting");
        String joinUrl = zoomClient.createMeeting(schoolId, topic, body.get("startTime"))
                .orElseThrow(() -> new BusinessException("Could not create the Zoom meeting — check the connection is configured correctly"));
        return ResponseEntity.ok(ApiResponse.ok(Map.of("joinUrl", joinUrl)));
    }

    /** Publishes one real snapshot row of this school's live KPIs to the configured Power BI
     * push dataset. */
    @PostMapping("/powerbi/publish")
    public ResponseEntity<ApiResponse<Void>> publishToPowerBi(
            @PathVariable String schoolId, @PathVariable String code, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        long enrollment = studentRepository.countActiveBySchoolId(schoolId);
        Double feesCollected = feePaymentRepository.sumCollected(schoolId);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Enrollment", enrollment);
        row.put("FeesCollectedTotal", feesCollected != null ? feesCollected : 0);

        boolean published = powerBiClient.publishSnapshot(schoolId, row);
        if (!published) {
            throw new BusinessException("Could not publish to Power BI — check the connection is configured correctly and the push dataset/table exists");
        }
        return ResponseEntity.ok(ApiResponse.ok("Snapshot published", null));
    }

    /** Best-effort candidate sync — see EczSyncClient's class-level note on why this contract is
     * provisional pending ECZ's own API documentation. */
    @PostMapping("/ecz/sync")
    public ResponseEntity<ApiResponse<String>> syncEcz(
            @PathVariable String schoolId, @PathVariable String code, @RequestBody(required = false) Object candidatesPayload, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        String response = eczSyncClient.syncCandidates(schoolId, candidatesPayload == null ? Map.of() : candidatesPayload)
                .orElseThrow(() -> new BusinessException("ECZ sync failed — check the endpoint URL and token, and confirm this school's ECZ integration contract with ECZ directly"));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
