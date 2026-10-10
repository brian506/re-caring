package com.recaring.location.implement.detection;

import com.recaring.location.vo.AnomalyAlert;
import com.recaring.location.vo.AnomalyAlertKind;
import com.recaring.location.vo.AnomalyEvent;
import com.recaring.location.vo.AnomalyMeasures;
import com.recaring.location.vo.AnomalyReasonCode;
import com.recaring.location.vo.DetectionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AnomalyDetectionParser {

    private static final DateTimeFormatter RECORDED_AT_FORMAT = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .optionalStart().appendLiteral('T').append(DateTimeFormatter.ISO_LOCAL_TIME).optionalEnd()
            .optionalStart().appendLiteral(' ').append(DateTimeFormatter.ISO_LOCAL_TIME).optionalEnd()
            .toFormatter();

    private static final int MAX_EVIDENCE_LENGTH = 1000;

    public Optional<AnomalyAlert> parse(Map<String, String> fields) {
        Optional<DetectionType> detectionType = DetectionType.find(fields.get("detection_type"));
        if (detectionType.isEmpty()) {
            log.warn("[이상탐지 결과 : 알 수 없는 유형]: detectionType={}", fields.get("detection_type"));
            return Optional.empty();
        }

        String wardMemberKey = fields.get("ward_member_key");
        if (wardMemberKey == null || wardMemberKey.isBlank()) {
            log.warn("[이상탐지 결과 : 회원 식별자 없음]: detectionType={}", detectionType.get());
            return Optional.empty();
        }

        String alertKind = fields.get("alert_kind");
        Optional<AnomalyAlertKind> kind = AnomalyAlertKind.find(alertKind);
        if (alertKind != null && kind.isEmpty()) {
            log.warn("[이상탐지 결과 : 알 수 없는 알림 종류]: wardMemberKey={} | alertKind={}", wardMemberKey, alertKind);
            return Optional.empty();
        }
        if (kind.isPresent() && isBlank(fields.get("event_id"))) {
            log.warn("[이상탐지 결과 : 사건 식별자 없음]: wardMemberKey={} | alertKind={}", wardMemberKey, alertKind);
            return Optional.empty();
        }

        String recordedAt = fields.containsKey("recorded_at") ? fields.get("recorded_at") : fields.get("detected_at");

        try {
            return Optional.of(new AnomalyAlert(
                    wardMemberKey,
                    detectionType.get(),
                    Double.parseDouble(fields.get("score")),
                    LocalDateTime.parse(recordedAt, RECORDED_AT_FORMAT),
                    Double.parseDouble(fields.get("latitude")),
                    Double.parseDouble(fields.get("longitude")),
                    truncate(fields.get("evidence")),
                    kind.map(found -> parseEvent(fields, found)).orElse(null),
                    parseReasonCode(wardMemberKey, fields.get("reason_code")),
                    parseMeasures(fields)
            ));
        } catch (NumberFormatException e) {
            log.warn("[이상탐지 결과 : 숫자 형식 오류]: wardMemberKey={} | error={}", wardMemberKey, e.getMessage());
            return Optional.empty();
        } catch (DateTimeParseException e) {
            log.warn("[이상탐지 결과 : 시각 형식 오류]: wardMemberKey={} | recordedAt={}",
                    wardMemberKey, recordedAt);
            return Optional.empty();
        } catch (NullPointerException e) {
            log.warn("[이상탐지 결과 : 필수 필드 누락]: wardMemberKey={} | fields={}", wardMemberKey, fields.keySet());
            return Optional.empty();
        }
    }

    private AnomalyEvent parseEvent(Map<String, String> fields, AnomalyAlertKind kind) {
        return new AnomalyEvent(
                fields.get("event_id"),
                kind,
                parseTime(fields.get("event_started_at")),
                parseTime(fields.get("event_ended_at")),
                fields.get("categories"),
                parseInteger(fields.get("remind_seq"))
        );
    }

    private AnomalyReasonCode parseReasonCode(String wardMemberKey, String value) {
        if (isBlank(value)) {
            return null;
        }
        Optional<AnomalyReasonCode> reasonCode = AnomalyReasonCode.find(value);
        if (reasonCode.isEmpty()) {
            log.warn("[이상탐지 결과 : 알 수 없는 사유 코드]: wardMemberKey={} | reasonCode={}", wardMemberKey, value);
        }
        return reasonCode.orElse(null);
    }

    private AnomalyMeasures parseMeasures(Map<String, String> fields) {
        return new AnomalyMeasures(
                parseInteger(fields.get("duration_min")),
                parseInteger(fields.get("usual_min")),
                parseInteger(fields.get("speed_kmh")),
                parseInteger(fields.get("hour"))
        );
    }

    private Integer parseInteger(String value) {
        if (isBlank(value)) {
            return null;
        }
        return (int) Math.round(Double.parseDouble(value));
    }

    private LocalDateTime parseTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        return LocalDateTime.parse(value, RECORDED_AT_FORMAT);
    }

    private String truncate(String evidence) {
        if (isBlank(evidence)) {
            return null;
        }
        if (evidence.length() <= MAX_EVIDENCE_LENGTH) {
            return evidence;
        }
        log.warn("[이상탐지 결과 : 근거 문구 초과]: length={}", evidence.length());
        return evidence.substring(0, MAX_EVIDENCE_LENGTH);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
