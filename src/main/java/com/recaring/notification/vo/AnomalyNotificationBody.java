package com.recaring.notification.vo;

import com.recaring.location.vo.AnomalyAlert;
import com.recaring.location.vo.AnomalyMeasures;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public record AnomalyNotificationBody(String value) {

    private static final String NAME_PLACEHOLDER = "{name}";
    private static final String REMIND_PREFIX = "[확인 필요] ";
    private static final int MAX_LENGTH = 1000;
    private static final int MINUTES_PER_HOUR = 60;

    public static AnomalyNotificationBody of(AnomalyAlert alert, String wardName) {
        String body = describe(alert, wardName);
        if (alert.isRemind()) {
            body = REMIND_PREFIX + body;
        }
        return new AnomalyNotificationBody(body.length() <= MAX_LENGTH ? body : body.substring(0, MAX_LENGTH));
    }

    private static String describe(AnomalyAlert alert, String wardName) {
        String subject = wardName + "님이 ";
        AnomalyMeasures measures = alert.measures();
        if (alert.reasonCode() == null) {
            return fromEvidence(alert.evidence(), wardName);
        }
        return switch (alert.reasonCode()) {
            case VEHICLE -> sentence(subject + "차량으로 이동 중입니다", speed(measures), duration(measures));
            case DWELL_UNKNOWN -> sentence(subject + "낯선 곳에서 움직임이 없으십니다", duration(measures));
            case DWELL_AFTER_VEHICLE -> sentence(subject + "차에서 내린 뒤 낯선 곳에 머물고 계십니다", duration(measures));
            case DWELL_KNOWN_LONG -> sentence(subject + "평소보다 오래 한 곳에 머물고 계십니다",
                    duration(measures), usual(measures));
            case DWELL_SAFEZONE_LONG -> sentence(subject + "안심존 안 한 곳에 오래 머물고 계십니다", duration(measures));
            case OFF_ROUTE -> sentence(subject + "평소 다니지 않던 곳에 계십니다", duration(measures));
            case OUTSIDE_SAFEZONE -> sentence(subject + "안심존을 벗어나 계십니다", duration(measures));
            case UNFAMILIAR_AREA -> sentence(subject + "평소 생활 범위에서 벗어난 곳에 계십니다", duration(measures));
            case NIGHT_OUTING -> subject + "야간" + hourLabel(measures.hour()) + "에 외출하셨습니다.";
            case RARE_HOUR -> subject + "평소 나가지 않으시는 시각" + hourLabel(measures.hour()) + "에 외출하셨습니다.";
            case UNUSUAL_HOUR_AT_PLACE -> sentence(subject + "평소 이 시각에는 계시지 않던 곳에 계십니다", duration(measures));
            case NO_ARRIVAL -> fromEvidence(alert.evidence(), wardName);
        };
    }

    private static String fromEvidence(String evidence, String wardName) {
        if (evidence == null || evidence.isBlank()) {
            return wardName + "님에게 이상 징후가 감지되었어요.";
        }
        return evidence.replace(NAME_PLACEHOLDER, wardName);
    }

    private static String sentence(String text, String... details) {
        String joined = Arrays.stream(details)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        return joined.isEmpty() ? text + "." : text + " (" + joined + ").";
    }

    private static String speed(AnomalyMeasures measures) {
        return measures.speedKmh() == null ? null : "약 " + measures.speedKmh() + "km/h";
    }

    private static String duration(AnomalyMeasures measures) {
        return measures.durationMin() == null ? null : "지속 " + formatMinutes(measures.durationMin());
    }

    private static String usual(AnomalyMeasures measures) {
        return measures.usualMin() == null ? null : "평소 약 " + formatMinutes(measures.usualMin());
    }

    private static String hourLabel(Integer hour) {
        return hour == null ? "" : "(" + hour + "시경)";
    }

    private static String formatMinutes(int minutes) {
        int hours = minutes / MINUTES_PER_HOUR;
        int rest = minutes % MINUTES_PER_HOUR;
        if (hours == 0) {
            return rest + "분";
        }
        if (rest == 0) {
            return hours + "시간";
        }
        return hours + "시간 " + rest + "분";
    }
}
