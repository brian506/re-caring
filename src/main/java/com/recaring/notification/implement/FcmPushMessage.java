package com.recaring.notification.implement;

import java.util.Map;

public record FcmPushMessage(
        String title,
        String body,
        Map<String, String> dataPayload
) {
}
