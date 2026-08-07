package com.srms.api.security.tenant;

import jakarta.servlet.http.HttpServletRequest;

public final class TenantRequestAttributes {
    public static final String RESOLUTION = TenantRequestAttributes.class.getName() + ".resolution";
    public static final String EXPECTED_SCHOOL_ID = TenantRequestAttributes.class.getName() + ".expectedSchoolId";

    private TenantRequestAttributes() {}

    public static TenantResolution resolution(HttpServletRequest request) {
        Object value = request.getAttribute(RESOLUTION);
        return value instanceof TenantResolution resolution ? resolution : TenantResolution.development();
    }

    public static String expectedSchoolId(HttpServletRequest request) {
        Object value = request.getAttribute(EXPECTED_SCHOOL_ID);
        return value == null ? null : value.toString();
    }
}
