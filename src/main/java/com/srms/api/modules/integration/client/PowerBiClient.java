package com.srms.api.modules.integration.client;

import com.srms.api.modules.integration.entity.IntegrationConnection;
import com.srms.api.modules.integration.service.IntegrationService;
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
 * dashboard (https://learn.microsoft.com/en-us/rest/api/power-bi/push-datasets).
 *
 * Per-school credentials/config, mapped onto the generic IntegrationConnection fields:
 *   - accountId = Azure AD Tenant ID
 *   - clientId  = (reused: baseUrl field holds "{Azure AD App (client) ID}|{Power BI dataset ID}")
 *   - apiKey    = Azure AD App client secret
 *
 * Auth: Azure AD OAuth2 client-credentials grant, scope https://analysis.windows.net/powerbi/api/.default
 * — this requires the Azure AD app to have been granted a Power BI service-principal application
 * permission by the district's Power BI admin; that's an out-of-band admin step on their side, not
 * something this app can do for them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PowerBiClient {
    public static final String CODE = "powerbi";

    private final IntegrationService integrationService;
    private final RestTemplate restTemplate = new RestTemplate();

    private record Config(String tenantId, String clientId, String datasetId, String clientSecret) {}

    private Optional<Config> config(IntegrationConnection conn) {
        if (isBlank(conn.getAccountId()) || isBlank(conn.getBaseUrl()) || isBlank(conn.getApiKey())) return Optional.empty();
        String[] parts = conn.getBaseUrl().split("\\|", 2);
        if (parts.length != 2 || isBlank(parts[0]) || isBlank(parts[1])) return Optional.empty();
        return Optional.of(new Config(conn.getAccountId(), parts[0], parts[1], conn.getApiKey()));
    }

    private Optional<String> fetchToken(Config cfg) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", cfg.clientId());
            form.add("client_secret", cfg.clientSecret());
            form.add("scope", "https://analysis.windows.net/powerbi/api/.default");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String url = "https://login.microsoftonline.com/" + cfg.tenantId() + "/oauth2/v2.0/token";
            var response = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
            Object token = response.getBody() != null ? response.getBody().get("access_token") : null;
            return Optional.ofNullable(token).map(String::valueOf);
        } catch (Exception e) {
            log.warn("Power BI token fetch failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public IntegrationTestResult test(String schoolId) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty()) return IntegrationTestResult.fail("Not connected");
        Optional<Config> cfgOpt = config(connOpt.get());
        if (cfgOpt.isEmpty()) {
            return IntegrationTestResult.fail("Tenant ID (Account/merchant ID) and \"{App client ID}|{Dataset ID}\" (Base URL) and Client secret (API key) are all required");
        }
        Optional<String> token = fetchToken(cfgOpt.get());
        return token.isPresent()
                ? IntegrationTestResult.ok("Connected — Azure AD token issued for Power BI")
                : IntegrationTestResult.fail("Could not obtain an Azure AD token — check the tenant ID, app client ID, and client secret");
    }

    /** Pushes one snapshot row of live school KPIs into the configured push dataset table
     * (expected table name: "SchoolSnapshot", columns: Timestamp, SchoolId, Enrollment,
     * AttendanceRate, FeesCollected — the district sets this table up once in Power BI when they
     * create the push dataset). Returns true only if Power BI accepted the row. */
    public boolean publishSnapshot(String schoolId, Map<String, Object> row) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty()) return false;
        Optional<Config> cfgOpt = config(connOpt.get());
        if (cfgOpt.isEmpty()) return false;
        Config cfg = cfgOpt.get();
        Optional<String> token = fetchToken(cfg);
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

            String url = "https://api.powerbi.com/v1.0/myorg/datasets/" + cfg.datasetId() + "/tables/SchoolSnapshot/rows";
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
