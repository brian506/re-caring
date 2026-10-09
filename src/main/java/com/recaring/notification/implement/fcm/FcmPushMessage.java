package com.recaring.notification.implement.fcm;

import java.util.Map;

public record FcmPushMessage(
        String title,
        String body,
        Map<String, String> dataPayload,
        boolean dataOnly
) {
    public static FcmPushMessage notification(String title, String body, Map<String, String> dataPayload) {
        return new FcmPushMessage(title, body, dataPayload, false);
    }

    public static FcmPushMessage dataOnly(String title, String body, Map<String, String> dataPayload) {
        return new FcmPushMessage(title, body, dataPayload, true);
    }
}
