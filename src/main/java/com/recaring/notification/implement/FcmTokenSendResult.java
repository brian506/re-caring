package com.recaring.notification.implement;

public record FcmTokenSendResult(
        String token,
        boolean success,
        String messageId,
        String errorCode,
        String errorMessage,
        boolean retryable,
        boolean invalidToken,
        int attemptCount
) {
    public static FcmTokenSendResult sent(String token, String messageId) {
        return new FcmTokenSendResult(token, true, messageId, null, null, false, false, 1);
    }

    public static FcmTokenSendResult failed(
            String token,
            String errorCode,
            String errorMessage,
            boolean retryable,
            boolean invalidToken
    ) {
        return new FcmTokenSendResult(token, false, null, errorCode, errorMessage, retryable, invalidToken, 1);
    }
}
