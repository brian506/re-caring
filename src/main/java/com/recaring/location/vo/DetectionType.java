package com.recaring.location.vo;

import java.util.Arrays;
import java.util.Optional;

// 탐지 엔진이 판정하는 이상 유형. 위험도 순이며, 동시에 해당해도 엔진이 위쪽 하나만 발행한다.
public enum DetectionType {

    SPEED_ANOMALY("빠른 이동 알림"),
    WANDERING("배회 알림"),
    ABNORMAL_DWELLING("장시간 정지 알림"),
    ROUTE_DEVIATION("낯선 장소 알림"),
    TIME_ANOMALY("평소와 다른 시간 외출 알림");

    private final String notificationTitle;

    DetectionType(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    public String notificationTitle() {
        return notificationTitle;
    }

    // 모르는 값이면 예외 대신 비어 있는 결과를 준다. 엔진이 협의되지 않은 유형(SIGNAL_LOST 등)을 보내도
    // 소비 루프가 죽지 않고 그 메시지만 버릴 수 있어야 한다.
    public static Optional<DetectionType> find(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(value))
                .findFirst();
    }
}
