package com.srms.api.modules.integration.client;

import com.srms.api.exception.BusinessException;
import com.srms.api.modules.integration.service.IntegrationConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API client used for AI-assisted lesson plan drafting. Each school supplies
 * its own API key on its Integrations page (provider code "llm"), so AI usage and its cost stay
 * with that school. Deliberately never falls back to the platform's key.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnthropicClient {
    public static final String CODE = "llm";
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final String DEFAULT_MODEL = "claude-sonnet-5";
    private static final String API_VERSION = "2023-06-01";

    private final IntegrationConfigService config;
    private final RestTemplate restTemplate = new RestTemplate();

    public IntegrationTestResult test(String schoolId) {
        try {
            complete(schoolId, "Reply with the single word OK.", "Ping", 10);
            return IntegrationTestResult.ok("Connected — the model responded");
        } catch (BusinessException e) {
            return IntegrationTestResult.fail(e.getMessage());
        }
    }

    public boolean isConnected(String schoolId) {
        return config.schoolOwnCredential(CODE, schoolId, "apiKey").isPresent();
    }

    @SuppressWarnings("unchecked")
    public String complete(String schoolId, String system, String userMessage, int maxTokens) {
        String apiKey = config.schoolOwnCredential(CODE, schoolId, "apiKey").orElseThrow(() ->
                new BusinessException("AI lesson drafting isn't connected for this school yet — add your Anthropic API key on the Integrations page"));
        String baseUrl = config.schoolOwnConfig(CODE, schoolId, "apiBaseUrl").orElse(DEFAULT_BASE_URL);
        String model = config.schoolOwnConfig(CODE, schoolId, "model").orElse(DEFAULT_MODEL);

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", system,
                "messages", List.of(Map.of("role", "user", "content", userMessage)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", API_VERSION);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/v1/messages", new HttpEntity<>(body, headers), Map.class);
            if (response == null || !(response.get("content") instanceof List<?> blocks)) {
                throw new BusinessException("The AI service returned an empty response — try again");
            }
            StringBuilder text = new StringBuilder();
            for (Object block : blocks) {
                if (block instanceof Map<?, ?> m && "text".equals(m.get("type")) && m.get("text") != null) {
                    text.append(m.get("text"));
                }
            }
            return text.toString();
        } catch (HttpClientErrorException e) {
            log.warn("Anthropic rejected request for school {}: {}", schoolId, e.getStatusCode());
            throw new BusinessException("The AI service rejected the request (" + e.getStatusCode().value()
                    + ") — check the API key and model on the Integrations page");
        } catch (RestClientException e) {
            log.warn("Anthropic unreachable for school {}: {}", schoolId, e.getMessage());
            throw new BusinessException("The AI service is unreachable right now — try again shortly");
        }
    }
}
