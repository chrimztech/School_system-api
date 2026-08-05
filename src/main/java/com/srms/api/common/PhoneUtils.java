package com.srms.api.common;

/**
 * Canonicalizes Zambian phone numbers to one comparable digits-only format (2609XXXXXXXX)
 * regardless of how they were typed — with spaces, dashes, parentheses, a leading "+", a
 * leading "0", or bare without any prefix at all. Every place a phone number is stored or
 * compared (login lookup, guardian-phone linking, SMS dispatch) must run values through this
 * so "+260 977 000 000", "0977-000-000", and "977000000" are all recognized as the same number.
 */
public final class PhoneUtils {
    private static final String COUNTRY_CODE = "260";

    private PhoneUtils() {}

    public static String normalize(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return "";
        if (digits.startsWith("00" + COUNTRY_CODE)) {
            digits = digits.substring(2); // 00260977000000 -> 260977000000
        }
        if (digits.startsWith("0") && digits.length() == 10) {
            digits = COUNTRY_CODE + digits.substring(1); // 0977000000 -> 260977000000
        } else if (digits.length() == 9) {
            digits = COUNTRY_CODE + digits; // 977000000 -> 260977000000
        }
        // Anything else (already has the 260 country code, or an unrecognized length) is left as-is.
        return digits;
    }
}
