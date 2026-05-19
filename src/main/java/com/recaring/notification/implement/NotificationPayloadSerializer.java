package com.recaring.notification.implement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPayloadSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            log.error("[알림] payload 직렬화 실패", exception);
            throw new AppException(ErrorType.NOTIFICATION_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
