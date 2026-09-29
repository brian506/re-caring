package com.recaring.common.utils;

public class MaskingUtils {

    private MaskingUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
