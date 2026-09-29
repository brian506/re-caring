package com.recaring.notification.vo;

import com.recaring.location.vo.DetectionType;
import com.recaring.notification.dataaccess.entity.FeedbackReason;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public enum FeedbackCategory {

    ANOMALY(EnumSet.allOf(FeedbackReason.class)),
    SAFE_ZONE(EnumSet.of(FeedbackReason.GPS_INACCURATE, FeedbackReason.OTHER));

    private final Set<FeedbackReason> allowedReasons;

    FeedbackCategory(Set<FeedbackReason> allowedReasons) {
        this.allowedReasons = allowedReasons;
    }

    public static Optional<FeedbackCategory> find(String eventType) {
        if (DetectionType.find(eventType).isPresent()) {
            return Optional.of(ANOMALY);
        }
        if (SafeZoneAlertType.find(eventType).isPresent()) {
            return Optional.of(SAFE_ZONE);
        }
        return Optional.empty();
    }

    public boolean allows(FeedbackReason reason) {
        return reason == null || allowedReasons.contains(reason);
    }
}
