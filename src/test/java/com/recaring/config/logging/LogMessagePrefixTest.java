package com.recaring.config.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogMessagePrefix 메시지 접두어 파싱")
class LogMessagePrefixTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "NULL", value = {
            "[GPS 수신 : 저장 완료]: wardKey=abc       | GPS 수신   | 저장 완료",
            "[SSE 이벤트:전송 실패] extra             | SSE 이벤트 | 전송 실패",
            "[  Redis 캐시  :   직렬화 실패  ]       | Redis 캐시 | 직렬화 실패",
            "[REQUEST API] GET /api/v1/x            | REQUEST API | NULL",
            "[이상탐지 결과 : 처리 실패] [nested : x] | 이상탐지 결과 | 처리 실패"
    })
    @DisplayName("선두 [카테고리 : 상세] 에서 category와 detail을 추출한다")
    void parse_leading_bracket(String message, String category, String detail) {
        LogMessagePrefix prefix = LogMessagePrefix.parse(message);

        assertThat(prefix).isNotNull();
        assertThat(prefix.category()).isEqualTo(category);
        assertThat(prefix.detail()).isEqualTo(detail);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "Started RecaringApplication in 3.2 seconds",
            " [GPS 수신 : 저장 완료]",
            "HikariPool-1 [GPS 수신 : 저장 완료]",
            "[]",
            "[ : 상세만]",
            "[닫히지 않은 괄호 : 상세"
    })
    @DisplayName("선두가 유효한 대괄호 접두어가 아니면 null을 반환한다")
    void parse_returns_null_without_leading_prefix(String message) {
        assertThat(LogMessagePrefix.parse(message)).isNull();
    }
}
