package com.recaring.common.utils;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public class MaskingUtils {

    private MaskingUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new AppException(ErrorType.EMAIL_IS_NULL);
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];

        if (localPart.length() <= 3) {
            return localPart.charAt(0) + "****@" + domainPart;
        }

        String visibleId = localPart.substring(0, 3);
        return visibleId + "****@" + domainPart;
    }
}
