package com.srms.api.common;

import java.util.Set;

/**
 * A bulk import that couldn't capture a real guardian name for a pupil often still writes a
 * placeholder like "Not Provided", paired with a fallback contact (a front-office number, say)
 * rather than a real one. Every place that treats a matching guardian phone/email as proof a
 * parent account owns a particular pupil's records must first check the guardian name isn't one
 * of these — otherwise a shared fallback contact reused across dozens of otherwise-unrelated
 * pupils would let one parent's login see every one of those children's private records, not
 * just their own child's.
 */
public final class GuardianNames {
    private static final Set<String> PLACEHOLDERS = Set.of(
            "", "not provided", "not available", "n/a", "na", "unknown", "unavailable",
            "tbd", "none", "-", "pending", "guardian", "parent");

    private GuardianNames() {}

    public static boolean isPlaceholder(String name) {
        return PLACEHOLDERS.contains(name == null ? "" : name.trim().toLowerCase());
    }
}
