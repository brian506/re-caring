package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CarePartyRole;
import com.recaring.care.event.CareInvitationAcceptedEvent;
import com.recaring.care.event.CareInvitationSentEvent;
import com.recaring.care.fixture.CareFixture;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.implement.NotificationSendManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("케어 초대 알림 리스너 단위 테스트")
class CareInvitationNotificationListenerTest {

    @InjectMocks
    private CareInvitationNotificationListener listener;

    @Mock
    private NotificationSendManager notificationSendManager;
    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("발송 이벤트 - 요청자 이름과 역할을 담아 초대 대상에게 알림을 보낸다")
    void onCareInvitationSent_sends_notification_to_target() {
        Member requester = CareFixture.createGuardianMember(); // name = "보호자"
        CareInvitationSentEvent event = new CareInvitationSentEvent(
                CareFixture.REQUEST_KEY,
                CareFixture.WARD_MEMBER_KEY,
                CareFixture.GUARDIAN_MEMBER_KEY,
                CarePartyRole.WARD
        );
        given(memberReader.findByMemberKey(CareFixture.GUARDIAN_MEMBER_KEY)).willReturn(requester);

        listener.onCareInvitationSent(event);

        ArgumentCaptor<String> memberKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

        then(notificationSendManager).should().sendToMember(
                memberKeyCaptor.capture(),
                eventTypeCaptor.capture(),
                titleCaptor.capture(),
                bodyCaptor.capture(),
                payloadCaptor.capture()
        );

        assertThat(memberKeyCaptor.getValue()).isEqualTo(CareFixture.WARD_MEMBER_KEY);
        assertThat(eventTypeCaptor.getValue()).isEqualTo("CARE_INVITATION_SENT");
        assertThat(titleCaptor.getValue()).isEqualTo("새로운 보호 대상자 요청");
        assertThat(bodyCaptor.getValue()).isEqualTo("보호자님이 보호 대상자로 요청을 보냈어요.");
        assertThat(payloadCaptor.getValue())
                .containsEntry("type", "CARE_INVITATION_SENT")
                .containsEntry("requestKey", CareFixture.REQUEST_KEY)
                .containsEntry("role", CarePartyRole.WARD.name());
    }

    @Test
    @DisplayName("수락 이벤트 - 수락자 이름을 담아 요청자에게 알림을 보낸다")
    void onCareInvitationAccepted_sends_notification_to_requester() {
        Member acceptor = CareFixture.createWardMember(); // name = "보호대상자"
        CareInvitationAcceptedEvent event = new CareInvitationAcceptedEvent(
                CareFixture.REQUEST_KEY,
                CareFixture.WARD_MEMBER_KEY,
                CareFixture.GUARDIAN_MEMBER_KEY,
                CarePartyRole.WARD
        );
        given(memberReader.findByMemberKey(CareFixture.WARD_MEMBER_KEY)).willReturn(acceptor);

        listener.onCareInvitationAccepted(event);

        ArgumentCaptor<String> memberKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

        then(notificationSendManager).should().sendToMember(
                memberKeyCaptor.capture(),
                eventTypeCaptor.capture(),
                titleCaptor.capture(),
                bodyCaptor.capture(),
                payloadCaptor.capture()
        );

        assertThat(memberKeyCaptor.getValue()).isEqualTo(CareFixture.GUARDIAN_MEMBER_KEY);
        assertThat(eventTypeCaptor.getValue()).isEqualTo("CARE_INVITATION_ACCEPTED");
        assertThat(titleCaptor.getValue()).isEqualTo("요청 수락");
        assertThat(bodyCaptor.getValue()).isEqualTo("보호대상자님이 요청을 수락했어요.");
        assertThat(payloadCaptor.getValue())
                .containsEntry("type", "CARE_INVITATION_ACCEPTED")
                .containsEntry("requestKey", CareFixture.REQUEST_KEY)
                .containsEntry("role", CarePartyRole.WARD.name());
    }
}
