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
@DisplayName("알림 전달 매니저 단위 테스트")
class NotificationDeliveryManagerTest {

    @InjectMocks
    private NotificationDeliveryManager notificationDeliveryManager;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Test
    @DisplayName("토큰별 요청 상태 전달 이력을 생성한다")
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
    @DisplayName("성공 및 재시도 가능한 실패 FCM 결과를 반영한다")
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
    @DisplayName("재시도 불가능한 유효하지 않은 토큰 결과를 반영한다")
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

    @Test
    @DisplayName("FCM 결과가 누락되면 기존 시도 횟수에서 증가시킨다")
    void applyResults_increments_attempt_count_when_result_is_missing() {
        Notification notification = notification();
        NotificationDelivery delivery = NotificationDelivery.requested(
                notification,
                NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN)
        );
        delivery.markFailed("OLD_FAILURE", "old failure", 2, true, LocalDateTime.now());
        FcmSendResult sendResult = new FcmSendResult(List.of());

        notificationDeliveryManager.applyResults(List.of(delivery), sendResult);

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(delivery.getFailureCode()).isEqualTo("MISSING_FCM_RESULT");
        assertThat(delivery.getAttemptCount()).isEqualTo(3);
        assertThat(delivery.isRetryable()).isTrue();
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
