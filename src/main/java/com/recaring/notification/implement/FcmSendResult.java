package com.recaring.notification.implement;

import java.util.List;

public record FcmSendResult(
        List<FcmTokenSendResult> tokenResults
) {
    public List<String> invalidTokens() {
        return tokenResults.stream()
                .filter(FcmTokenSendResult::invalidToken)
                .map(FcmTokenSendResult::token)
                .toList();
    }
}
