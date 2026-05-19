package com.recaring.notification.implement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 payload 직렬화 단위 테스트")
class NotificationPayloadSerializerTest {

    @InjectMocks
    private NotificationPayloadSerializer notificationPayloadSerializer;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("payload를 JSON 문자열로 직렬화한다")
    void serialize_returns_json() throws JsonProcessingException {
        Map<String, String> payload = Map.of("wardKey", NotificationFixture.WARD_KEY);
        given(objectMapper.writeValueAsString(payload)).willReturn("{\"wardKey\":\"ward-member-key-001\"}");

        String result = notificationPayloadSerializer.serialize(payload);

        assertThat(result).isEqualTo("{\"wardKey\":\"ward-member-key-001\"}");
    }

    @Test
    @DisplayName("직렬화에 실패하면 원인을 보존한 AppException을 던진다")
    void serialize_throws_app_exception_with_cause() throws JsonProcessingException {
        Map<String, String> payload = Map.of("wardKey", NotificationFixture.WARD_KEY);
        JsonProcessingException cause = new JsonProcessingException("serialize failed") {
        };
        given(objectMapper.writeValueAsString(payload)).willThrow(cause);

        assertThatThrownBy(() -> notificationPayloadSerializer.serialize(payload))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> {
                    AppException appException = (AppException) exception;
                    assertThat(appException.getErrorType())
                            .isEqualTo(ErrorType.NOTIFICATION_PAYLOAD_SERIALIZATION_FAILED);
                    assertThat(appException.getCause()).isSameAs(cause);
                    assertThat(appException.getData()).isNull();
                });
    }
}
