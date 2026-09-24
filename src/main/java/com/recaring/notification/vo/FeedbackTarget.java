package com.recaring.notification.vo;

import com.recaring.location.vo.DetectionType;
import com.recaring.notification.dataaccess.entity.Notification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

public record FeedbackTarget(
        Long notificationId,
        String recipientMemberKey,
        String eventType,
        DetectionType detectionType,
        String wardMemberKey,
        LocalDateTime recordedAt
) {
    private static final DateTimeFormatter RECORDED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String WARD_KEY = "wardKey";
    private static final String RECORDED_AT = "recordedAt";

    public static FeedbackTarget from(Notification notification) {
        Map<String, String> payload = notification.getDataPayload() == null
                ? Map.of()
                : notification.getDataPayload();
        return new FeedbackTarget(
                notification.getId(),
                notification.getRecipientMemberKey(),
                notification.getEventType(),
                DetectionType.find(notification.getEventType()).orElse(null),
                payload.get(WARD_KEY),
                parseRecordedAt(payload.get(RECORDED_AT))
        );
    }

    public boolean isRecipient(String memberKey) {
        return recipientMemberKey.equals(memberKey);
    }

    public boolean isFeedbackEligible() {
        return detectionType != null;
    }

    public boolean hasDetectionKeys() {
        return wardMemberKey != null && recordedAt != null && isFeedbackEligible();
    }

    private static LocalDateTime parseRecordedAt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, RECORDED_AT_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
