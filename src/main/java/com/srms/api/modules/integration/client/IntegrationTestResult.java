package com.srms.api.modules.integration.client;

/** Outcome of a real, live call made against a third-party provider using a school's saved
 * credentials — never a simulated/assumed result. */
public record IntegrationTestResult(boolean success, String message) {
    public static IntegrationTestResult ok(String message) {
        return new IntegrationTestResult(true, message);
    }

    public static IntegrationTestResult fail(String message) {
        return new IntegrationTestResult(false, message);
    }
}
