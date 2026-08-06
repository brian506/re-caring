package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.implement.fcm.FcmClient;
import com.recaring.notification.implement.fcm.FcmDeviceTokenManager;
import com.recaring.notification.implement.fcm.FcmDeviceTokenReader;
import com.recaring.notification.implement.fcm.FcmPushMessage;
import com.recaring.notification.fixture.NotificationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 전송 매니저 단위 테스트")
class NotificationSendManagerTest {

    @InjectMocks
    private NotificationSendManager notificationSendManager;

    @Mock
    private FcmDeviceTokenReader fcmDeviceTokenReader;
    @Mock
    private FcmDeviceTokenManager fcmDeviceTokenManager;
    @Mock
    private NotificationWriter notificationWriter;
    @Mock
    private FcmClient fcmClient;

    @Test
    @DisplayName("브로드캐스트 - 보호자/관계자 전원에게 알림을 저장하고 토큰으로 FCM을 발송한다")
    void sendToCareParties_records_for_all_recipients_and_broadcasts() {
        List<String> guardianKeys = List.of(NotificationFixture.GUARDIAN_KEY);
        List<String> managerKeys = List.of(NotificationFixture.MANAGER_KEY);
        List<FcmDeviceToken> tokens = List.of(
                NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN),
                NotificationFixture.managerFcmDeviceToken(NotificationFixture.MANAGER_FCM_TOKEN)
        );
        given(fcmDeviceTokenReader.findTokensByCareRoles(guardianKeys, managerKeys)).willReturn(tokens);
        given(fcmClient.sendAndCollectInvalidTokens(any(FcmPushMessage.class), anyList())).willReturn(List.of());

        notificationSendManager.sendToCareParties(
                guardianKeys, managerKeys, "SAFE_ZONE_EXIT", "title", "body", Map.of("wardKey", NotificationFixture.WARD_KEY));

        then(notificationWriter).should().addAll(
                List.of(NotificationFixture.GUARDIAN_KEY, NotificationFixture.MANAGER_KEY),
                "SAFE_ZONE_EXIT", "title", "body", Map.of("wardKey", NotificationFixture.WARD_KEY));
        then(fcmClient).should().sendAndCollectInvalidTokens(any(FcmPushMessage.class), any());
        then(fcmDeviceTokenManager).should().deleteInvalidTokens(List.of());
    }

    @Test
    @DisplayName("특정 회원 발송 - 해당 회원에게 알림을 저장하고 회원 토큰으로 FCM을 발송한다")
    void sendToMember_records_and_sends_to_member_tokens() {
        List<FcmDeviceToken> tokens = List.of(
                NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN)
        );
        given(fcmDeviceTokenReader.findTokensByMemberKey(NotificationFixture.GUARDIAN_KEY))
                .willReturn(tokens);
        given(fcmClient.sendAndCollectInvalidTokens(any(FcmPushMessage.class), anyList())).willReturn(List.of());

        notificationSendManager.sendToMember(
                NotificationFixture.GUARDIAN_KEY,
                "CARE_INVITATION_SENT",
                "새로운 케어 요청",
                "홍길동님이 보호자로 케어 요청을 보냈어요.",
                Map.of("type", "CARE_INVITATION_SENT")
        );

        then(notificationWriter).should().addAll(
                List.of(NotificationFixture.GUARDIAN_KEY),
                "CARE_INVITATION_SENT",
                "새로운 케어 요청",
                "홍길동님이 보호자로 케어 요청을 보냈어요.",
                Map.of("type", "CARE_INVITATION_SENT"));

        ArgumentCaptor<List<String>> tokenCaptor = ArgumentCaptor.forClass(List.class);
        then(fcmClient).should().sendAndCollectInvalidTokens(any(FcmPushMessage.class), tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).containsExactly(NotificationFixture.GUARDIAN_FCM_TOKEN);
    }

    @Test
    @DisplayName("무효 토큰이 반환되면 해당 토큰을 삭제한다")
    void dispatch_deletes_invalid_tokens() {
        List<String> guardianKeys = List.of(NotificationFixture.GUARDIAN_KEY);
        List<String> managerKeys = List.of(NotificationFixture.MANAGER_KEY);
        List<FcmDeviceToken> tokens = List.of(
                NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN)
        );
        given(fcmDeviceTokenReader.findTokensByCareRoles(guardianKeys, managerKeys)).willReturn(tokens);
        given(fcmClient.sendAndCollectInvalidTokens(any(FcmPushMessage.class), anyList()))
                .willReturn(List.of(NotificationFixture.GUARDIAN_FCM_TOKEN));

        notificationSendManager.sendToCareParties(
                guardianKeys, managerKeys, "SAFE_ZONE_EXIT", "title", "body", Map.of("wardKey", NotificationFixture.WARD_KEY));

        then(fcmDeviceTokenManager).should().deleteInvalidTokens(List.of(NotificationFixture.GUARDIAN_FCM_TOKEN));
    }

    @Test
    @DisplayName("발송 대상 토큰이 없어도 알림은 저장하고 FCM은 호출하지 않는다")
    void records_notification_even_when_no_tokens() {
        given(fcmDeviceTokenReader.findTokensByMemberKey(NotificationFixture.GUARDIAN_KEY))
                .willReturn(List.of());

        notificationSendManager.sendToMember(
                NotificationFixture.GUARDIAN_KEY,
                "CARE_INVITATION_SENT",
                "새로운 케어 요청",
                "본문",
                Map.of()
        );

        then(notificationWriter).should().addAll(
                List.of(NotificationFixture.GUARDIAN_KEY),
                "CARE_INVITATION_SENT", "새로운 케어 요청", "본문", Map.of());
        then(fcmClient).shouldHaveNoInteractions();
        then(fcmDeviceTokenManager).shouldHaveNoInteractions();
    }
}
