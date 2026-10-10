package com.recaring.location.vo;

import java.time.LocalDateTime;

public record AnomalyEvent(
        String eventId,
        AnomalyAlertKind alertKind,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String categories,
        Integer remindSeq
) {
}
