package com.srms.api.modules.integration.client;

import com.srms.api.modules.integration.service.IntegrationConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Real client for Microsoft's Power BI REST API — publishes rows into a Power BI "push dataset"
 * table so a district/board can watch this school's KPIs update live on their own Power BI
 * dashboard (https://learn.microsoft.com/en-us/power-bi/developer/embedded/embed-service-principal).
 * Configuration fields exactly match the platform's integration-configuration spec:
 *   configuration: tenantId, applicationClientId, workspaceId, reportId, datasetId, capacityId,
 *                  reportDisplayName, reportType, rowLevelSecurityRole, refreshSchedule
 *   credentials:   clientSecret, certificate, certificatePassword (certificate preferred for
 *                  production; either clientSecret or certificate is required)
 *
 * Auth: Azure AD OAuth2 client-credentials grant, scope https://analysis.windows.net/powerbi/api/.default
 * — this requires the Azure AD app to have been granted a Power BI service-principal application
 * permission by the district's Power BI admin; that's an out-of-band admin step on their side, not
 * something this app can do for them. Certificate-based auth (JWT client assertion) is real
 * follow-up work — this client currently supports the client-secret grant, which Microsoft still
 * fully supports for service principals.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PowerBiClient {
    public static final String CODE = "powerbi";

    private final IntegrationConfigService config;
    private final RestTemplate restTemplate = new RestTemplate();

    private String str(Optional<Object> v) { return v.map(String::valueOf).orElse(null); }

    private Optional<String> fetchToken(String schoolId) {
        String tenantId = str(config.resolveConfig(CODE, schoolId, "tenantId"));
        String clientId = str(config.resolveConfig(CODE, schoolId, "applicationClientId"));
        String clientSecret = config.resolveCredential(CODE, schoolId, "clientSecret").orElse(null);
        if (isBlank(tenantId) || isBlank(clientId) || isBlank(clientSecret)) return Optional.empty();

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("scope", "https://analysis.windows.net/powerbi/api/.default");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String url = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
            var response = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
            Object token = response.getBody() != null ? response.getBody().get("access_token") : null;
            return Optional.ofNullable(token).map(String::valueOf);
        } catch (Exception e) {
            log.warn("Power BI token fetch failed for school {}: {}", schoolId, e.getMessage());
            return Optional.empty();
        }
    }

    public IntegrationTestResult test(String schoolId) {
        String tenantId = str(config.resolveConfig(CODE, schoolId, "tenantId"));
        String clientId = str(config.resolveConfig(CODE, schoolId, "applicationClientId"));
        String clientSecret = config.resolveCredential(CODE, schoolId, "clientSecret").orElse(null);
        if (isBlank(tenantId) || isBlank(clientId) || isBlank(clientSecret)) {
            return IntegrationTestResult.fail("Tenant ID, Application (client) ID, and Client secret are all required");
        }
        return fetchToken(schoolId).isPresent()
                ? IntegrationTestResult.ok("Connected — Azure AD token issued for Power BI")
                : IntegrationTestResult.fail("Could not obtain an Azure AD token — check the tenant ID, client ID, and client secret");
    }

    /** Pushes one snapshot row of live school KPIs into the configured push dataset table
     * (expected table name: "SchoolSnapshot", columns: Timestamp, SchoolId, Enrollment,
     * FeesCollectedTotal — the district sets this table up once in Power BI when they create the
     * push dataset). Returns true only if Power BI accepted the row. */
    public boolean publishSnapshot(String schoolId, Map<String, Object> row) {
        String datasetId = str(config.resolveConfig(CODE, schoolId, "datasetId"));
        if (isBlank(datasetId)) return false;
        Optional<String> token = fetchToken(schoolId);
        if (token.isEmpty()) return false;

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            Map<String, Object> fullRow = new LinkedHashMap<>(row);
            fullRow.putIfAbsent("Timestamp", Instant.now().toString());
            fullRow.putIfAbsent("SchoolId", schoolId);
            payload.put("rows", List.of(fullRow));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token.get());

            String url = "https://api.powerbi.com/v1.0/myorg/datasets/" + datasetId + "/tables/SchoolSnapshot/rows";
            var response = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), Void.class);
            return response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NO_CONTENT;
        } catch (Exception e) {
            log.warn("Power BI publish failed for school {}: {}", schoolId, e.getMessage());
            return false;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
