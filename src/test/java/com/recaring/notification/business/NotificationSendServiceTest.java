package com.recaring.notification.business;

import com.recaring.notification.business.command.NotificationSendCommand;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.entity.NotificationDelivery;
import com.recaring.notification.dataaccess.entity.NotificationStatus;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.FcmClient;
import com.recaring.notification.implement.FcmDeviceTokenManager;
import com.recaring.notification.implement.FcmDeviceTokenReader;
import com.recaring.notification.implement.FcmSendResult;
import com.recaring.notification.implement.FcmTokenSendResult;
import com.recaring.notification.implement.NotificationDeliveryManager;
import com.recaring.notification.implement.NotificationPayloadSerializer;
import com.recaring.notification.implement.NotificationWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 발송 서비스 단위 테스트")
class NotificationSendServiceTest {

    @InjectMocks
    private NotificationSendService notificationSendService;

    @Mock
    private NotificationPayloadSerializer notificationPayloadSerializer;
    @Mock
    private FcmDeviceTokenReader fcmDeviceTokenReader;
    @Mock
    private FcmDeviceTokenManager fcmDeviceTokenManager;
    @Mock
    private NotificationWriter notificationWriter;
    @Mock
    private NotificationDeliveryManager notificationDeliveryManager;
    @Mock
    private FcmClient fcmClient;

    @Test
    @DisplayName("FCM을 발송하고 전달 결과를 저장한다")
    void send_sends_fcm_and_records_results() {
        NotificationSendCommand command = NotificationFixture.sendCommand();
        Notification notification = Notification.builder()
                .eventType(command.eventType())
                .title(command.title())
                .body(command.body())
                .dataPayload("{\"wardKey\":\"ward-member-key-001\"}")
                .build();
        FcmDeviceToken guardianToken =
                NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN);
        FcmDeviceToken managerToken =
                NotificationFixture.managerFcmDeviceToken(NotificationFixture.MANAGER_FCM_TOKEN);
        List<FcmDeviceToken> tokens = List.of(guardianToken, managerToken);
        List<NotificationDelivery> deliveries = tokens.stream()
                .map(token -> NotificationDelivery.requested(notification, token))
                .toList();
        FcmSendResult sendResult = new FcmSendResult(List.of(
                FcmTokenSendResult.sent(NotificationFixture.GUARDIAN_FCM_TOKEN, "message-id"),
                FcmTokenSendResult.failed(NotificationFixture.MANAGER_FCM_TOKEN, "UNAVAILABLE", "temporary", true, false)
        ));

        given(notificationPayloadSerializer.serialize(command.dataPayload()))
                .willReturn("{\"wardKey\":\"ward-member-key-001\"}");
        given(notificationWriter.addRequested(any(), any(), any(), any())).willReturn(notification);
        given(fcmDeviceTokenReader.findActiveRecipientTokens(command.guardianMemberKeys(), command.managerMemberKeys()))
                .willReturn(tokens);
        given(notificationDeliveryManager.addRequested(notification, tokens)).willReturn(deliveries);
        given(fcmClient.send(any(), any())).willReturn(sendResult);

        NotificationSendResult result = notificationSendService.send(command);

        then(notificationDeliveryManager).should().applyResults(deliveries, sendResult);
        then(fcmDeviceTokenManager).should().deactivateInvalidTokens(List.of());
        then(fcmDeviceTokenManager).should().touchLastUsedAt(List.of(
                NotificationFixture.GUARDIAN_FCM_TOKEN,
                NotificationFixture.MANAGER_FCM_TOKEN
        ));
        then(notificationWriter).should().completeByDeliveries(notification, deliveries);
        assertThat(result.notificationKey()).isEqualTo(notification.getNotificationKey());
    }

    @Test
    @DisplayName("활성 토큰이 없으면 알림을 실패로 처리한다")
    void send_marks_failed_when_no_tokens() {
        NotificationSendCommand command = NotificationFixture.sendCommand();
        Notification notification = Notification.builder()
                .eventType(command.eventType())
                .title(command.title())
                .body(command.body())
                .dataPayload("{}")
                .build();
        notification.markFailed("NO_ACTIVE_TOKEN", "No active FCM device token was found.");

        given(notificationPayloadSerializer.serialize(command.dataPayload())).willReturn("{}");
        given(notificationWriter.addRequested(any(), any(), any(), any())).willReturn(notification);
        given(fcmDeviceTokenReader.findActiveRecipientTokens(command.guardianMemberKeys(), command.managerMemberKeys()))
                .willReturn(List.of());

        NotificationSendResult result = notificationSendService.send(command);

        then(notificationWriter).should().markFailedForNoActiveToken(notification);
        then(fcmClient).shouldHaveNoInteractions();
        assertThat(result.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(result.requestedCount()).isZero();
    }
}
