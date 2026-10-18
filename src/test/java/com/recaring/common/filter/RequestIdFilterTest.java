package com.recaring.common.filter;

import com.recaring.common.fixture.CommonFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("요청 ID 필터 통합 테스트")
class RequestIdFilterTest extends AbstractIntegrationTest {

    private static final String PUBLIC_PATH = "/actuator/health";
    private static final String PROTECTED_PATH = "/api/v1/members/me";

    @Test
    @DisplayName("헤더 없이 요청하면 UUID 형식의 요청 ID가 응답 헤더에 담긴다")
    void issues_uuid_when_header_is_absent() {
        String requestId = client.get()
                .uri(PUBLIC_PATH)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseHeaders()
                .getFirst(CommonFixture.REQUEST_ID_HEADER);

        assertThat(requestId).matches(CommonFixture.UUID_PATTERN);
    }

    @Test
    @DisplayName("유효한 요청 ID를 보내면 같은 값이 응답 헤더로 돌아온다")
    void echoes_valid_client_request_id() {
        String requestId = requestIdFor(CommonFixture.CLIENT_REQUEST_ID);

        assertThat(requestId).isEqualTo(CommonFixture.CLIENT_REQUEST_ID);
    }

    @Test
    @DisplayName("8자 요청 ID는 그대로 쓴다")
    void keeps_request_id_of_minimum_length() {
        String requestId = requestIdFor(CommonFixture.REQUEST_ID_8_CHARS);

        assertThat(requestId).isEqualTo(CommonFixture.REQUEST_ID_8_CHARS);
    }

    @Test
    @DisplayName("7자 요청 ID는 버리고 새 UUID를 발급한다")
    void replaces_request_id_shorter_than_minimum() {
        String requestId = requestIdFor(CommonFixture.REQUEST_ID_7_CHARS);

        assertThat(requestId).matches(CommonFixture.UUID_PATTERN);
    }

    @Test
    @DisplayName("64자 요청 ID는 그대로 쓴다")
    void keeps_request_id_of_maximum_length() {
        String requestId = requestIdFor(CommonFixture.REQUEST_ID_64_CHARS);

        assertThat(requestId).isEqualTo(CommonFixture.REQUEST_ID_64_CHARS);
    }

    @Test
    @DisplayName("65자 요청 ID는 버리고 새 UUID를 발급한다")
    void replaces_request_id_longer_than_maximum() {
        String requestId = requestIdFor(CommonFixture.REQUEST_ID_65_CHARS);

        assertThat(requestId).matches(CommonFixture.UUID_PATTERN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"request id 1234", "request_id_1234", "abcd1234|level=ERROR"})
    @DisplayName("영숫자·하이픈 외 문자가 섞인 요청 ID는 로그 위조를 막기 위해 새 UUID로 바꾼다")
    void replaces_request_id_with_disallowed_characters(String clientRequestId) {
        String requestId = requestIdFor(clientRequestId);

        assertThat(requestId).matches(CommonFixture.UUID_PATTERN);
    }

    @Test
    @DisplayName("인증 실패로 보안 필터에서 끝난 응답에도 요청 ID가 담긴다")
    void includes_request_id_on_unauthorized_response() {
        String requestId = client.get()
                .uri(PROTECTED_PATH)
                .header(CommonFixture.REQUEST_ID_HEADER, CommonFixture.CLIENT_REQUEST_ID)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .returnResult()
                .getResponseHeaders()
                .getFirst(CommonFixture.REQUEST_ID_HEADER);

        assertThat(requestId).isEqualTo(CommonFixture.CLIENT_REQUEST_ID);
    }

    @Test
    @DisplayName("헤더 없이 보낸 요청마다 서로 다른 요청 ID가 발급된다")
    void issues_distinct_request_ids_per_request() {
        String first = issuedRequestId();

        String second = issuedRequestId();

        assertThat(second).isNotEqualTo(first);
    }

    private String requestIdFor(String clientRequestId) {
        return client.get()
                .uri(PUBLIC_PATH)
                .header(CommonFixture.REQUEST_ID_HEADER, clientRequestId)
                .exchange()
                .expectBody()
                .returnResult()
                .getResponseHeaders()
                .getFirst(CommonFixture.REQUEST_ID_HEADER);
    }

    private String issuedRequestId() {
        return client.get()
                .uri(PUBLIC_PATH)
                .exchange()
                .expectBody()
                .returnResult()
                .getResponseHeaders()
                .getFirst(CommonFixture.REQUEST_ID_HEADER);
    }
}
