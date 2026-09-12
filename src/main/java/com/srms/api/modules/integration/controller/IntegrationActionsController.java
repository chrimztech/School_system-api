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
import com.srms.api.modules.integration.dto.IntegrationConfigView;
import com.srms.api.modules.integration.entity.IntegrationConfig;
import com.srms.api.modules.integration.entity.IntegrationEventLog;
import com.srms.api.modules.integration.entity.PowerBiReport;
import com.srms.api.modules.integration.repository.PowerBiReportRepository;
import com.srms.api.modules.integration.service.IntegrationConfigService;
import com.srms.api.modules.payment.service.ZynlePayClient;
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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Real, live actions for the school-scoped integrations configured via IntegrationConfigController
 * — separate from that CRUD, which only ever stores credentials. Everything here makes an actual
 * outbound call to the provider using those credentials, and every attempt (success or failure)
 * is recorded to the integration's event log (see IntegrationConfigService.recordEvent).
 */
@RestController
@RequestMapping("/api/schools/{schoolId}/integration-configs")
@RequiredArgsConstructor
public class IntegrationActionsController {
    private final IntegrationConfigService configService;
    private final AfricasTalkingSmsClient africasTalkingSmsClient;
    private final MtnMomoClient mtnMomoClient;
    private final AirtelMoneyClient airtelMoneyClient;
    private final PowerBiClient powerBiClient;
    private final ZoomClient zoomClient;
    private final EczSyncClient eczSyncClient;
    private final GoogleWorkspaceClient googleWorkspaceClient;
    private final ZynlePayClient zynlePayClient;
    private final StudentRepository studentRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final PowerBiReportRepository powerBiReportRepository;

    /** Runs a real, safe (non-money-moving, non-destructive) call against the provider using the
     * school's saved credentials, and records the outcome as the connection's live status. */
    @PostMapping("/{code}/test")
    public ResponseEntity<ApiResponse<IntegrationConfigView>> test(
            @PathVariable String schoolId, @PathVariable String code, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        IntegrationTestResult result = switch (code) {
            case AfricasTalkingSmsClient.CODE -> africasTalkingSmsClient.test(schoolId);
            case MtnMomoClient.CODE -> mtnMomoClient.test(schoolId);
            case AirtelMoneyClient.CODE -> airtelMoneyClient.test(schoolId);
            case PowerBiClient.CODE -> powerBiClient.test(schoolId);
            case ZoomClient.CODE -> zoomClient.test(schoolId);
            case EczSyncClient.CODE -> eczSyncClient.test(schoolId);
            case GoogleWorkspaceClient.CODE -> googleWorkspaceClient.test(schoolId);
            case ZynlePayClient.CODE -> zynlePayClient.isConfigured(schoolId)
                    ? IntegrationTestResult.ok("Merchant ID, API ID, and API key are all present")
                    : IntegrationTestResult.fail("Merchant ID, API ID, and API key are all required");
            default -> throw new BusinessException("No live test is available for \"" + code + "\" yet");
        };
        configService.recordTestOutcome(IntegrationConfig.ScopeType.SCHOOL, schoolId, code, result.success(), result.message());
        if (!result.success()) {
            throw new BusinessException(result.message());
        }
        Optional<IntegrationConfigView> updated = configService.get(IntegrationConfig.ScopeType.SCHOOL, schoolId, code);
        return ResponseEntity.ok(ApiResponse.ok(updated.orElse(null)));
    }

    /** Creates a real, scheduled Zoom meeting and returns its join URL. */
    @PostMapping("/zoom/create-meeting")
    public ResponseEntity<ApiResponse<Map<String, String>>> createZoomMeeting(
            @PathVariable String schoolId, @RequestBody Map<String, String> body, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        String topic = body.getOrDefault("topic", "SRMS meeting");
        Optional<String> joinUrl = zoomClient.createMeeting(schoolId, topic, body.get("startTime"));
        configService.recordEvent(IntegrationConfig.ScopeType.SCHOOL, schoolId, ZoomClient.CODE, IntegrationEventLog.EventType.MEETING_CREATED,
                joinUrl.isPresent(), joinUrl.isPresent() ? "Meeting \"" + topic + "\" created" : "Could not create the meeting",
                joinUrl.map(u -> "{\"topic\":\"" + topic.replace("\"", "'") + "\",\"joinUrl\":\"" + u + "\"}").orElse(null), auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("joinUrl", joinUrl.orElseThrow(
                () -> new BusinessException("Could not create the Zoom meeting — check the connection is configured correctly")))));
    }

    /** Publishes one real snapshot row of this school's live KPIs to the given report's Power BI
     * push dataset (reportId from the "id" field in the request body — see PowerBiReportController
     * for the list of reports this school has). */
    @PostMapping("/powerbi/publish")
    public ResponseEntity<ApiResponse<Void>> publishToPowerBi(
            @PathVariable String schoolId, @RequestBody Map<String, String> body, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        String reportId = body.get("id");
        PowerBiReport report = powerBiReportRepository.findByIdAndScopeTypeAndSchoolId(reportId, IntegrationConfig.ScopeType.SCHOOL, schoolId)
                .orElseThrow(() -> new BusinessException("Report not found — save it first"));

        long enrollment = studentRepository.countActiveBySchoolId(schoolId);
        Double feesCollected = feePaymentRepository.sumCollected(schoolId);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Enrollment", enrollment);
        row.put("FeesCollectedTotal", feesCollected != null ? feesCollected : 0);

        boolean published = powerBiClient.publishSnapshot(schoolId, report.getDatasetId(), row);
        if (published) {
            report.setLastRefreshAt(LocalDateTime.now());
            powerBiReportRepository.save(report);
        }
        configService.recordEvent(IntegrationConfig.ScopeType.SCHOOL, schoolId, PowerBiClient.CODE, IntegrationEventLog.EventType.PUBLISH,
                published, published ? "Published to \"" + report.getDisplayName() + "\"" : "Could not publish to \"" + report.getDisplayName() + "\"",
                null, auth.getName());
        if (!published) {
            throw new BusinessException("Could not publish to Power BI — check the connection is configured correctly and the push dataset/table exists");
        }
        return ResponseEntity.ok(ApiResponse.ok("Snapshot published", null));
    }

    /** Best-effort candidate sync — see EczSyncClient's class-level note on why this contract is
     * provisional pending ECZ's own API documentation. */
    @PostMapping("/ecz/sync")
    public ResponseEntity<ApiResponse<String>> syncEcz(
            @PathVariable String schoolId, @RequestBody(required = false) Object candidatesPayload, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        Optional<String> response = eczSyncClient.syncCandidates(schoolId, candidatesPayload == null ? Map.of() : candidatesPayload);
        configService.recordEvent(IntegrationConfig.ScopeType.SCHOOL, schoolId, EczSyncClient.CODE, IntegrationEventLog.EventType.SYNC,
                response.isPresent(), response.isPresent() ? "Sync request sent" : "Sync failed", null, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response.orElseThrow(
                () -> new BusinessException("ECZ sync failed — check the endpoint URL and token, and confirm this school's ECZ integration contract with ECZ directly"))));
    }
}
