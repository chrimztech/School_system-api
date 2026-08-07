package com.srms.api.security.tenant;

public record TenantResolution(Scope scope, String schoolId, String slug) {
    public enum Scope { PLATFORM, TENANT, DEVELOPMENT }

    public static TenantResolution platform() {
        return new TenantResolution(Scope.PLATFORM, null, null);
    }

    public static TenantResolution development() {
        return new TenantResolution(Scope.DEVELOPMENT, null, null);
    }

    public static TenantResolution tenant(String schoolId, String slug) {
        return new TenantResolution(Scope.TENANT, schoolId, slug);
    }

    public boolean isTenant() {
        return scope == Scope.TENANT;
    }
}
