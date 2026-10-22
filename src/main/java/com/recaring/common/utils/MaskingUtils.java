package com.recaring.common.utils;

import java.util.regex.Pattern;

public class MaskingUtils {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(01[016789])[-\\s]?\\d{3,4}[-\\s]?(\\d{4})");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_\\-:.]{32,}");
    private static final int MAX_EXTERNAL_TEXT_LENGTH = 300;

    private MaskingUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskExternalText(String text) {
        if (text == null) {
            return null;
        }
        String masked = TOKEN_PATTERN.matcher(text).replaceAll("[REDACTED]");
        masked = PHONE_PATTERN.matcher(masked).replaceAll("$1****$2");
        if (masked.length() > MAX_EXTERNAL_TEXT_LENGTH) {
            return masked.substring(0, MAX_EXTERNAL_TEXT_LENGTH) + "...(truncated)";
        }
        return masked;
    }
}
