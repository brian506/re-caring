package com.recaring.location.vo;

import java.time.LocalDateTime;
import java.util.Optional;

// event·reasonCode가 null이면 사건 단위 출력 이전 형식(evidence만 있는 메시지)이다.
public record AnomalyAlert(
        String wardMemberKey,
        DetectionType detectionType,
        double score,
        LocalDateTime recordedAt,
        double latitude,
        double longitude,
        String evidence,
        AnomalyEvent event,
        AnomalyReasonCode reasonCode,
        AnomalyMeasures measures
) {
    public boolean isPushed() {
        if (event == null) {
            return true;
        }
        if (reasonCode != null && !reasonCode.isPushed()) {
            return false;
        }
        if (isRemind() && reasonCode != null && !reasonCode.isRemindPushed()) {
            return false;
        }
        return event.alertKind().isPushed();
    }

    public String notificationTitle() {
        return Optional.ofNullable(reasonCode)
                .flatMap(code -> code.notificationTitle(isRemind()))
                .orElseGet(detectionType::notificationTitle);
    }

    public boolean isRemind() {
        return event != null && event.alertKind() == AnomalyAlertKind.REMIND;
    }

    public boolean isEventEnd() {
        return event != null && event.alertKind() == AnomalyAlertKind.EVENT_END;
    }
}
