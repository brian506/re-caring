package com.recaring.notification.implement;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(FirebaseMessaging.class)
public class FirebaseFcmClient implements FcmClient {

    private static final int MULTICAST_LIMIT = 500;

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public FcmSendResult send(FcmPushMessage message, List<String> tokens) {
        List<FcmTokenSendResult> results = new ArrayList<>();
        for (int start = 0; start < tokens.size(); start += MULTICAST_LIMIT) {
            int end = Math.min(start + MULTICAST_LIMIT, tokens.size());
            results.addAll(sendBatch(message, tokens.subList(start, end)));
        }
        return new FcmSendResult(results);
    }

    private List<FcmTokenSendResult> sendBatch(FcmPushMessage message, List<String> tokens) {
        MulticastMessage multicastMessage = MulticastMessage.builder()
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(message.dataPayload())
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(multicastMessage);
            return parseBatchResponse(tokens, response);
        } catch (FirebaseMessagingException exception) {
            return tokens.stream()
                    .map(token -> FcmTokenSendResult.failed(
                            token,
                            errorCode(exception),
                            exception.getMessage(),
                            isRetryable(exception),
                            isInvalidToken(exception)
                    ))
                    .toList();
        } catch (RuntimeException exception) {
            return tokens.stream()
                    .map(token -> FcmTokenSendResult.failed(
                            token,
                            "FCM_SEND_EXCEPTION",
                            exception.getMessage(),
                            true,
                            false
                    ))
                    .toList();
        }
    }

    private List<FcmTokenSendResult> parseBatchResponse(List<String> tokens, BatchResponse response) {
        List<FcmTokenSendResult> results = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int index = 0; index < responses.size(); index++) {
            SendResponse sendResponse = responses.get(index);
            String token = tokens.get(index);
            if (sendResponse.isSuccessful()) {
                results.add(FcmTokenSendResult.sent(token, sendResponse.getMessageId()));
                continue;
            }

            FirebaseMessagingException exception = sendResponse.getException();
            results.add(FcmTokenSendResult.failed(
                    token,
                    errorCode(exception),
                    exception.getMessage(),
                    isRetryable(exception),
                    isInvalidToken(exception)
            ));
        }
        return results;
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

    private boolean isRetryable(FirebaseMessagingException exception) {
        if (isInvalidToken(exception)) {
            return false;
        }
        ErrorCode code = exception.getErrorCode();
        return code == ErrorCode.UNAVAILABLE || code == ErrorCode.INTERNAL || code == ErrorCode.DEADLINE_EXCEEDED;
    }
}
