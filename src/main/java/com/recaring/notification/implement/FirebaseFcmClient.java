package com.recaring.notification.implement;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(FirebaseMessaging.class)
public class FirebaseFcmClient implements FcmClient {

    private static final int MAX_ATTEMPTS = 3;

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public List<String> sendAndCollectInvalidTokens(FcmPushMessage message, List<String> tokens) {
        List<String> invalidTokens = new ArrayList<>();
        for (String token : tokens) {
            if (isInvalidAfterSend(message, token)) {
                invalidTokens.add(token);
            }
        }
        return invalidTokens;
    }

    private boolean isInvalidAfterSend(FcmPushMessage message, String token) {
        Message fcmMessage = Message.builder()
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(message.dataPayload())
                .setToken(token)
                .build();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                firebaseMessaging.send(fcmMessage);
                return false;
            } catch (FirebaseMessagingException exception) {
                if (isInvalidToken(exception)) {
                    log.warn("[알림 전송 : 무효 토큰]: errorCode={}", errorCode(exception));
                    return true;
                }
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("[알림 전송 : 전송 실패]: errorCode={} | attempts={} | error={}",
                            errorCode(exception), attempt, exception.getMessage());
                    return false;
                }
            } catch (RuntimeException exception) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("[알림 전송 : 예외]: attempts={} | error={}", attempt, exception.getMessage());
                    return false;
                }
            }
        }
        return false;
    }

    private String errorCode(FirebaseMessagingException exception) {
        MessagingErrorCode messagingErrorCode = exception.getMessagingErrorCode();
        if (messagingErrorCode != null) {
            return messagingErrorCode.name();
        }

        ErrorCode errorCode = exception.getErrorCode();
        return errorCode == null ? "UNKNOWN" : errorCode.name();
    }

    private boolean isInvalidToken(FirebaseMessagingException exception) {
        MessagingErrorCode code = exception.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
