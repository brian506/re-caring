package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.entity.NotificationDelivery;
import com.recaring.notification.dataaccess.entity.NotificationDeliveryStatus;
import com.recaring.notification.dataaccess.repository.NotificationDeliveryRepository;
import com.recaring.notification.fixture.NotificationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDeliveryManager unit test")
class NotificationDeliveryManagerTest {

    @InjectMocks
    private NotificationDeliveryManager notificationDeliveryManager;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Test
    @DisplayName("Creates requested deliveries for tokens")
    void addRequested_creates_requested_deliveries() {
        Notification notification = notification();
        FcmDeviceToken guardianToken =
                NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN);
        List<FcmDeviceToken> tokens = List.of(guardianToken);
        given(notificationDeliveryRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<NotificationDelivery> deliveries = notificationDeliveryManager.addRequested(notification, tokens);

        then(notificationDeliveryRepository).should().saveAll(org.mockito.ArgumentMatchers.anyList());
        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.getFirst().getStatus()).isEqualTo(NotificationDeliveryStatus.REQUESTED);
        assertThat(deliveries.getFirst().getTokenSnapshot()).isEqualTo(NotificationFixture.GUARDIAN_FCM_TOKEN);
    }

    @Test
    @DisplayName("Applies sent and retryable failed FCM results")
    void applyResults_marks_sent_and_retryable_failed() {
        Notification notification = notification();
        NotificationDelivery sentDelivery = NotificationDelivery.requested(
                notification,
                NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN)
        );
        NotificationDelivery failedDelivery = NotificationDelivery.requested(
                notification,
                NotificationFixture.managerFcmDeviceToken(NotificationFixture.MANAGER_FCM_TOKEN)
        );
        FcmSendResult sendResult = new FcmSendResult(List.of(
                FcmTokenSendResult.sent(NotificationFixture.GUARDIAN_FCM_TOKEN, "fcm-message-id"),
                FcmTokenSendResult.failed(NotificationFixture.MANAGER_FCM_TOKEN, "UNAVAILABLE", "temporary", true, false)
        ));

        notificationDeliveryManager.applyResults(List.of(sentDelivery, failedDelivery), sendResult);

        assertThat(sentDelivery.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(sentDelivery.getFcmMessageId()).isEqualTo("fcm-message-id");
        assertThat(sentDelivery.isRetryable()).isFalse();

        assertThat(failedDelivery.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(failedDelivery.getFailureCode()).isEqualTo("UNAVAILABLE");
        assertThat(failedDelivery.isRetryable()).isTrue();
        assertThat(failedDelivery.getNextRetryAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Applies non-retryable invalid token FCM result")
    void applyResults_marks_invalid_token_failed_without_retry() {
        Notification notification = notification();
        NotificationDelivery delivery = NotificationDelivery.requested(
                notification,
                NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN)
        );
        FcmSendResult sendResult = new FcmSendResult(List.of(
                FcmTokenSendResult.failed(NotificationFixture.GUARDIAN_FCM_TOKEN, "UNREGISTERED", "gone", false, true)
        ));

        notificationDeliveryManager.applyResults(List.of(delivery), sendResult);

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(delivery.getFailureCode()).isEqualTo("UNREGISTERED");
        assertThat(delivery.isRetryable()).isFalse();
        assertThat(delivery.getNextRetryAt()).isNull();
    }

    private Notification notification() {
        return Notification.builder()
                .eventType("SAFE_ZONE_EXIT")
                .title("title")
                .body("body")
                .dataPayload("{}")
                .build();
    }
}
