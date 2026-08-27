package com.recaring.notification.implement.fcm;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(FirebaseMessaging.class)
public class FirebaseFcmClient implements FcmClient {

    private static final int MAX_ATTEMPTS = 2;

    // MulticastMessage.build()가 500개를 넘기면 IllegalArgumentException을 던진다.
    private static final int MAX_TOKENS_PER_REQUEST = 500;

    // 503은 SDK가 이미 4회 재시도한다. 여기서 다시 잡는 것은 그마저 소진된 뒤의 마지막 한 번이다.
    private static final Set<ErrorCode> RETRYABLE_ERROR_CODES =
            EnumSet.of(ErrorCode.UNAVAILABLE, ErrorCode.INTERNAL, ErrorCode.DEADLINE_EXCEEDED);

    private static final Set<MessagingErrorCode> INVALID_TOKEN_ERROR_CODES =
            EnumSet.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT);

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public List<String> sendAndCollectInvalidTokens(FcmPushMessage message, List<String> tokens) {
        List<String> invalidTokens = new ArrayList<>();
        for (int start = 0; start < tokens.size(); start += MAX_TOKENS_PER_REQUEST) {
            int end = Math.min(start + MAX_TOKENS_PER_REQUEST, tokens.size());
            invalidTokens.addAll(sendChunk(message, tokens.subList(start, end)));
        }
        return invalidTokens;
    }

    private List<String> sendChunk(FcmPushMessage message, List<String> tokens) {
        List<String> invalidTokens = new ArrayList<>();
        List<String> pendingTokens = tokens;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS && !pendingTokens.isEmpty(); attempt++) {
            BatchResponse batchResponse = send(message, pendingTokens, attempt);
            if (batchResponse == null) {
                return invalidTokens;
            }
            pendingTokens = collectRetryable(batchResponse, pendingTokens, invalidTokens, attempt);
        }

        if (!pendingTokens.isEmpty()) {
            log.warn("[알림 전송 : 재시도 소진]: tokens={} | attempts={}", pendingTokens.size(), MAX_ATTEMPTS);
        }
        return invalidTokens;
    }

    private List<String> collectRetryable(BatchResponse batchResponse, List<String> tokens,
                                          List<String> invalidTokens, int attempt) {
        List<SendResponse> responses = batchResponse.getResponses();
        List<String> retryableTokens = new ArrayList<>();

        for (int index = 0; index < responses.size(); index++) {
            SendResponse response = responses.get(index);
            if (response.isSuccessful()) {
                continue;
            }

            String token = tokens.get(index);
            FirebaseMessagingException exception = response.getException();
            if (isInvalidToken(exception)) {
                log.warn("[알림 전송 : 무효 토큰]: errorCode={}", errorCode(exception));
                invalidTokens.add(token);
                continue;
            }
            if (isRetryable(exception)) {
                retryableTokens.add(token);
                continue;
            }
            log.warn("[알림 전송 : 전송 실패]: errorCode={} | attempt={} | error={}",
                    errorCode(exception), attempt, exception.getMessage());
        }
        return retryableTokens;
    }

    private BatchResponse send(FcmPushMessage message, List<String> tokens, int attempt) {
        MulticastMessage multicastMessage = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(message.dataPayload())
                .addAllTokens(tokens)
                .build();

        try {
            return firebaseMessaging.sendEachForMulticast(multicastMessage);
        } catch (FirebaseMessagingException exception) {
            log.warn("[알림 전송 : 일괄 전송 실패]: errorCode={} | tokens={} | attempt={} | error={}",
                    errorCode(exception), tokens.size(), attempt, exception.getMessage());
            return null;
        } catch (RuntimeException exception) {
            log.error("[알림 전송 : 예외]: tokens={} | attempt={} | error={}",
                    tokens.size(), attempt, exception.getMessage());
            return null;
        }
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
        return code != null && INVALID_TOKEN_ERROR_CODES.contains(code);
    }

    private boolean isRetryable(FirebaseMessagingException exception) {
        ErrorCode code = exception.getErrorCode();
        return code != null && RETRYABLE_ERROR_CODES.contains(code);
    }
}
