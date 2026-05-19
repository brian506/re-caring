package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.entity.NotificationDelivery;
import com.recaring.notification.dataaccess.entity.NotificationStatus;
import com.recaring.notification.dataaccess.repository.NotificationRepository;
import com.recaring.notification.fixture.NotificationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 Writer 단위 테스트")
class NotificationWriterTest {

    @InjectMocks
    private NotificationWriter notificationWriter;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("활성 토큰이 없으면 알림 실패 상태를 저장한다")
    void markFailedForNoActiveToken_saves_failed_notification() {
        Notification notification = notification();

        notificationWriter.markFailedForNoActiveToken(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getFailureCode()).isEqualTo("NO_ACTIVE_TOKEN");
        then(notificationRepository).should().save(notification);
    }

    @Test
    @DisplayName("모든 전달이 성공하면 알림 성공 상태를 저장한다")
    void completeByDeliveries_saves_sent_notification() {
        Notification notification = notification();
        NotificationDelivery delivery = delivery(notification);
        delivery.markSent("message-id", 1);

        notificationWriter.completeByDeliveries(notification, List.of(delivery));

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        then(notificationRepository).should().save(notification);
    }

    @Test
    @DisplayName("일부 전달만 성공하면 알림 부분 성공 상태를 저장한다")
    void completeByDeliveries_saves_partial_notification() {
        Notification notification = notification();
        NotificationDelivery sentDelivery = delivery(notification);
        NotificationDelivery failedDelivery = NotificationDelivery.requested(
                notification,
                NotificationFixture.managerFcmDeviceToken(NotificationFixture.MANAGER_FCM_TOKEN)
        );
        sentDelivery.markSent("message-id", 1);
        failedDelivery.markFailed("UNAVAILABLE", "temporary", 1, true, null);

        notificationWriter.completeByDeliveries(notification, List.of(sentDelivery, failedDelivery));

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PARTIAL);
        assertThat(notification.getFailureCode()).isEqualTo("DELIVERY_FAILED");
        then(notificationRepository).should().save(notification);
    }

    @Test
    @DisplayName("모든 전달이 실패하면 알림 실패 상태를 저장한다")
    void completeByDeliveries_saves_failed_notification() {
        Notification notification = notification();
        NotificationDelivery delivery = delivery(notification);
        delivery.markFailed("UNAVAILABLE", "temporary", 1, true, null);

        notificationWriter.completeByDeliveries(notification, List.of(delivery));

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getFailureCode()).isEqualTo("DELIVERY_FAILED");
        then(notificationRepository).should().save(notification);
    }

    private Notification notification() {
        return Notification.builder()
                .eventType("SAFE_ZONE_EXIT")
                .title("title")
                .body("body")
                .dataPayload("{}")
                .build();
    }

    private NotificationDelivery delivery(Notification notification) {
        FcmDeviceToken token = NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN);
        return NotificationDelivery.requested(notification, token);
    }
}
