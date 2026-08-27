package com.recaring.location.implement.detection;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.AnomalyAlert;
import com.recaring.location.vo.DetectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("이상탐지 결과 파싱 단위 테스트")
class AnomalyDetectionParserTest {

    private static final int MAX_EVIDENCE_LENGTH = 1000;
    private static final String EVIDENCE = LocationFixture.WANDERING_EVIDENCE;

    private final AnomalyDetectionParser anomalyDetectionParser = new AnomalyDetectionParser();

    @Test
    @DisplayName("규격에 맞는 메시지는 탐지 결과로 읽어 들인다")
    void parses_well_formed_message() {
        Optional<AnomalyAlert> result = anomalyDetectionParser.parse(LocationFixture.createAnomalyStreamFields(DetectionType.WANDERING, EVIDENCE));

        assertThat(result).isPresent();
        AnomalyAlert alert = result.get();
        assertThat(alert.wardMemberKey()).isEqualTo(LocationFixture.WARD_KEY);
        assertThat(alert.detectionType()).isEqualTo(DetectionType.WANDERING);
        assertThat(alert.score()).isEqualTo(LocationFixture.ANOMALY_SCORE);
        assertThat(alert.detectedAt()).isEqualTo(LocationFixture.DETECTED_AT);
        assertThat(alert.latitude()).isEqualTo(LocationFixture.LATITUDE);
        assertThat(alert.longitude()).isEqualTo(LocationFixture.LONGITUDE);
        assertThat(alert.evidence()).isEqualTo(EVIDENCE);
    }

    @Test
    @DisplayName("협의되지 않은 탐지 유형이면 메시지를 버린다")
    void discards_unknown_detection_type() {
        Map<String, String> fields = LocationFixture.createAnomalyStreamFields(DetectionType.WANDERING, EVIDENCE);
        fields.put("detection_type", "SIGNAL_LOST");

        Optional<AnomalyAlert> result = anomalyDetectionParser.parse(fields);

        assertThat(result).isEmpty();
    }

    @ParameterizedTest(name = "대상자 식별자가 [{0}]이면 버린다")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("대상자 식별자가 없으면 메시지를 버린다")
    void discards_message_without_ward_member_key(String wardMemberKey) {
        Map<String, String> fields = LocationFixture.createAnomalyStreamFields(DetectionType.WANDERING, EVIDENCE);
        fields.put("ward_member_key", wardMemberKey);

        Optional<AnomalyAlert> result = anomalyDetectionParser.parse(fields);

        assertThat(result).isEmpty();
    }

    @ParameterizedTest(name = "{0}이 숫자가 아니면 버린다")
    @ValueSource(strings = {"score", "latitude", "longitude"})
    @DisplayName("숫자 필드 형식이 어긋나면 메시지를 버린다")
    void discards_message_with_non_numeric_field(String field) {
        Map<String, String> fields = LocationFixture.createAnomalyStreamFields(DetectionType.WANDERING, EVIDENCE);
        fields.put(field, "not-a-number");

        Optional<AnomalyAlert> result = anomalyDetectionParser.parse(fields);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("탐지 시각 형식이 규격과 다르면 메시지를 버린다")
    void discards_message_with_malformed_detected_at() {
        Map<String, String> fields = LocationFixture.createAnomalyStreamFields(DetectionType.WANDERING, EVIDENCE);
        fields.put("detected_at", "2026-07-27T10:15:03+09:00");

        Optional<AnomalyAlert> result = anomalyDetectionParser.parse(fields);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("필수 필드가 아예 빠져 있으면 메시지를 버린다")
    void discards_message_missing_required_field() {
        Map<String, String> fields = LocationFixture.createAnomalyStreamFields(DetectionType.WANDERING, EVIDENCE);
        fields.remove("score");

        Optional<AnomalyAlert> result = anomalyDetectionParser.parse(fields);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("근거 문구가 1000자면 그대로 둔다")
    void keeps_evidence_at_the_length_limit() {
        Map<String, String> fields = LocationFixture.createAnomalyStreamFields(DetectionType.WANDERING, EVIDENCE);
        fields.put("evidence", "가".repeat(MAX_EVIDENCE_LENGTH));

        Optional<AnomalyAlert> result = anomalyDetectionParser.parse(fields);

        assertThat(result).isPresent();
        assertThat(result.get().evidence()).hasSize(MAX_EVIDENCE_LENGTH);
    }

    @Test
    @DisplayName("근거 문구가 1000자를 넘으면 1000자로 자른다")
    void truncates_evidence_beyond_the_length_limit() {
        Map<String, String> fields = LocationFixture.createAnomalyStreamFields(DetectionType.WANDERING, EVIDENCE);
        fields.put("evidence", "가".repeat(MAX_EVIDENCE_LENGTH + 1));

        Optional<AnomalyAlert> result = anomalyDetectionParser.parse(fields);

        assertThat(result).isPresent();
        assertThat(result.get().evidence()).hasSize(MAX_EVIDENCE_LENGTH);
    }

    @ParameterizedTest(name = "근거 문구가 [{0}]이면 빈 문자열이 된다")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("근거 문구가 비어 있어도 메시지를 버리지 않는다")
    void keeps_message_with_blank_evidence(String evidence) {
        Map<String, String> fields = LocationFixture.createAnomalyStreamFields(DetectionType.WANDERING, EVIDENCE);
        fields.put("evidence", evidence);

        Optional<AnomalyAlert> result = anomalyDetectionParser.parse(fields);

        assertThat(result).isPresent();
        assertThat(result.get().evidence()).isEmpty();
    }

}
