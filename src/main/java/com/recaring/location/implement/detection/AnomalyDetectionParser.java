package com.recaring.location.implement.detection;

import com.recaring.location.vo.AnomalyAlert;
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

    // 엔진은 'T' 구분자로 발행한다. 공백 구분자도 함께 받되, 오프셋이 붙은 값은 거부한다.
    private static final DateTimeFormatter DETECTED_AT_FORMAT = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .optionalStart().appendLiteral('T').optionalEnd()
            .optionalStart().appendLiteral(' ').optionalEnd()
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
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

        try {
            return Optional.of(new AnomalyAlert(
                    wardMemberKey,
                    detectionType.get(),
                    Double.parseDouble(fields.get("score")),
                    LocalDateTime.parse(fields.get("detected_at"), DETECTED_AT_FORMAT),
                    Double.parseDouble(fields.get("latitude")),
                    Double.parseDouble(fields.get("longitude")),
                    truncate(fields.get("evidence"))
            ));
        } catch (NumberFormatException e) {
            log.warn("[이상탐지 결과 : 숫자 형식 오류]: wardMemberKey={} | error={}", wardMemberKey, e.getMessage());
            return Optional.empty();
        } catch (DateTimeParseException e) {
            log.warn("[이상탐지 결과 : 시각 형식 오류]: wardMemberKey={} | detectedAt={}",
                    wardMemberKey, fields.get("detected_at"));
            return Optional.empty();
        } catch (NullPointerException e) {
            log.warn("[이상탐지 결과 : 필수 필드 누락]: wardMemberKey={} | fields={}", wardMemberKey, fields.keySet());
            return Optional.empty();
        }
    }

    private String truncate(String evidence) {
        if (evidence == null || evidence.isBlank()) {
            return "";
        }
        if (evidence.length() <= MAX_EVIDENCE_LENGTH) {
            return evidence;
        }
        log.warn("[이상탐지 결과 : 근거 문구 초과]: length={}", evidence.length());
        return evidence.substring(0, MAX_EVIDENCE_LENGTH);
    }
}
