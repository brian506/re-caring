package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.entity.NotificationDelivery;
import com.recaring.notification.dataaccess.entity.NotificationDeliveryStatus;
import com.recaring.notification.dataaccess.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryManager {

    private static final int RETRY_DELAY_MINUTES = 5;

    private final NotificationDeliveryRepository notificationDeliveryRepository;

    @Transactional
    public List<NotificationDelivery> addRequested(Notification notification, List<FcmDeviceToken> tokens) {
        List<NotificationDelivery> deliveries = tokens.stream()
                .map(token -> NotificationDelivery.requested(notification, token))
                .toList();
        return notificationDeliveryRepository.saveAll(deliveries);
    }

    @Transactional
    public void applyResults(List<NotificationDelivery> deliveries, FcmSendResult sendResult) {
        Map<String, FcmTokenSendResult> resultByToken = sendResult.tokenResults().stream()
                .collect(Collectors.toMap(
                        FcmTokenSendResult::token,
                        Function.identity(),
                        (left, right) -> left
                ));

        deliveries.forEach(delivery -> {
            FcmTokenSendResult result = resultByToken.get(delivery.getTokenSnapshot());
            if (result == null) {
                delivery.markFailed("MISSING_FCM_RESULT", "FCM result was not returned.", 1, true, nextRetryAt());
                return;
            }
            if (result.success()) {
                delivery.markSent(result.messageId(), result.attemptCount());
                return;
            }
            delivery.markFailed(
                    result.errorCode(),
                    result.errorMessage(),
                    result.attemptCount(),
                    result.retryable(),
                    result.retryable() ? nextRetryAt() : null
            );
        });
    }

    public List<NotificationDelivery> findRetryTargets(LocalDateTime now) {
        return notificationDeliveryRepository.findAllByStatusAndRetryableTrueAndNextRetryAtLessThanEqual(
                NotificationDeliveryStatus.FAILED,
                now
        );
    }

    private LocalDateTime nextRetryAt() {
        return LocalDateTime.now().plusMinutes(RETRY_DELAY_MINUTES);
    }
}
