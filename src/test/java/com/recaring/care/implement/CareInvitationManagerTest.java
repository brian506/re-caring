package com.recaring.care.implement;

import com.recaring.care.vo.CareInvitationInfo;
import com.recaring.member.vo.MemberInfo;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.vo.CarePartyRole;
import com.recaring.care.event.CareInvitationAcceptedEvent;
import com.recaring.care.event.CareInvitationSentEvent;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.vo.CareRelationshipRegistration;
import com.recaring.member.implement.MemberReader;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CareInvitationManager 단위 테스트")
class CareInvitationManagerTest {

    @InjectMocks
    private CareInvitationManager careInvitationManager;

    @Mock
    private MemberReader memberReader;

    @Mock
    private CareInvitationReader careInvitationReader;

    @Mock
    private CareInvitationWriter careInvitationWriter;

    @Mock
    private CareRelationshipWriter careRelationshipWriter;

    @Mock
    private CareRelationshipValidator careRelationshipValidator;

    @Mock
    private CareInvitationResendLimiter careInvitationResendLimiter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("보호 대상자 초대 - 전화번호로 WARD 회원을 찾아 초대장을 등록한다")
    void sendWardInvitation_success() {
        MemberInfo ward = MemberInfo.from(CareFixture.createWardMember());
        CareInvitationInfo saved = CareInvitationInfo.from(CareFixture.createWardInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY));
        given(memberReader.findByPhone(new PhoneNumber(CareFixture.WARD_PHONE))).willReturn(ward);
        given(careInvitationWriter.register(any())).willReturn(saved);

        careInvitationManager.sendWardInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_PHONE);

        then(careRelationshipValidator).should(times(1))
                .validateCanAddWard(eq(CareFixture.GUARDIAN_MEMBER_KEY), eq(ward.memberKey()));
        then(careInvitationWriter).should(times(1)).register(any());
        then(eventPublisher).should(times(1)).publishEvent(any(CareInvitationSentEvent.class));
    }

    @Test
    @DisplayName("보호 대상자 초대 - WARD 역할이 아닌 회원을 추가하면 예외가 발생한다")
    void sendWardInvitation_fails_when_not_ward_role() {
        MemberInfo guardian = MemberInfo.from(CareFixture.createGuardianMember(CareFixture.WARD_PHONE));
        given(memberReader.findByPhone(new PhoneNumber(CareFixture.WARD_PHONE))).willReturn(guardian);

        assertThatThrownBy(() ->
                careInvitationManager.sendWardInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_PHONE))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_WARD_ROLE);

        then(careInvitationWriter).should(times(0)).register(any());
    }

    @Test
    @DisplayName("보호 대상자 초대 - 같은 요청자가 같은 대상자에게 보낸 대기 요청이 있으면 ALREADY_PENDING_CARE_REQUEST이고 등록하지 않는다")
    void sendWardInvitation_fails_when_same_requester_has_pending_request() {
        MemberInfo ward = MemberInfo.from(CareFixture.createWardMember());
        given(memberReader.findByPhone(new PhoneNumber(CareFixture.WARD_PHONE))).willReturn(ward);
        given(careInvitationReader.existsPendingWardRequest(CareFixture.GUARDIAN_MEMBER_KEY, ward.memberKey()))
                .willReturn(true);

        assertThatThrownBy(() ->
                careInvitationManager.sendWardInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_PHONE))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.ALREADY_PENDING_CARE_REQUEST);

        then(careInvitationWriter).should(times(0)).register(any());
    }

    @Test
    @DisplayName("초대 수락 - 케어 관계를 생성하고 초대장 상태를 수락으로 변경하며 이벤트를 발행한다")
    void accept_creates_relationship_and_updates_status() {
        CareInvitationInfo invitation = CareInvitationInfo.from(CareFixture.createWardInvitation(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY));
        given(careInvitationReader.findInvitationForRecipient(
                CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY)).willReturn(invitation);

        careInvitationManager.accept(CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY);

        then(careRelationshipWriter).should(times(1)).register(any(CareRelationshipRegistration.class));
        then(careInvitationWriter).should(times(1)).accept(anyString());
        then(eventPublisher).should(times(1)).publishEvent(any(CareInvitationAcceptedEvent.class));
    }

    @Test
    @DisplayName("초대 거절 - 초대장 상태를 거절로 변경한다")
    void reject_updates_status_to_rejected() {
        CareInvitationInfo invitation = CareInvitationInfo.from(CareFixture.createWardInvitation(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY));
        given(careInvitationReader.findInvitationForRecipient(
                CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY)).willReturn(invitation);

        careInvitationManager.reject(CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY);

        then(careInvitationWriter).should(times(1)).reject(anyString());
        then(careRelationshipWriter).should(times(0)).register(any());
    }

    @Test
    @DisplayName("초대 수락 - 이미 처리된(수락/거절) 초대장이면 예외가 발생한다")
    void accept_fails_when_invitation_already_processed() {
        CareInvitationInfo invitation = CareInvitationInfo.from(CareFixture.createAcceptedWardInvitation(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY));
        given(careInvitationReader.findInvitationForRecipient(
                CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY)).willReturn(invitation);

        assertThatThrownBy(() ->
                careInvitationManager.accept(CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.ALREADY_PROCESSED_CARE_REQUEST);

        then(careRelationshipWriter).should(times(0)).register(any());
        then(careInvitationWriter).should(times(0)).accept(anyString());
    }

    @Test
    @DisplayName("초대 거절 - 이미 처리된(수락/거절) 초대장이면 예외가 발생한다")
    void reject_fails_when_invitation_already_processed() {
        CareInvitationInfo invitation = CareInvitationInfo.from(CareFixture.createRejectedWardInvitation(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY));
        given(careInvitationReader.findInvitationForRecipient(
                CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY)).willReturn(invitation);

        assertThatThrownBy(() ->
                careInvitationManager.reject(CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.ALREADY_PROCESSED_CARE_REQUEST);

        then(careInvitationWriter).should(times(0)).reject(anyString());
    }

    @Test
    @DisplayName("재전송 - 대기 중인 요청이면 처음 보낸 요청의 키·수신자·역할로 발송 이벤트를 다시 발행한다")
    void resend_republishes_sent_event_for_pending_invitation() {
        CareInvitationInfo invitation = CareInvitationInfo.from(CareFixture.createGuardianInvitation(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.SECOND_GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY));
        given(careInvitationReader.findInvitationForRequester(
                CareFixture.REQUEST_KEY, CareFixture.GUARDIAN_MEMBER_KEY)).willReturn(invitation);

        careInvitationManager.resend(CareFixture.REQUEST_KEY, CareFixture.GUARDIAN_MEMBER_KEY);

        then(eventPublisher).should(times(1)).publishEvent(new CareInvitationSentEvent(
                invitation.requestKey(),
                CareFixture.SECOND_GUARDIAN_MEMBER_KEY,
                CareFixture.GUARDIAN_MEMBER_KEY,
                CarePartyRole.GUARDIAN));
    }

    @Test
    @DisplayName("재전송 - 간격 제한에 걸리면 발송 이벤트를 발행하지 않는다")
    void resend_does_not_publish_when_interval_limit_rejects() {
        CareInvitationInfo invitation = CareInvitationInfo.from(CareFixture.createWardInvitation(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY));
        given(careInvitationReader.findInvitationForRequester(
                CareFixture.REQUEST_KEY, CareFixture.GUARDIAN_MEMBER_KEY)).willReturn(invitation);
        willThrow(new AppException(ErrorType.CARE_REQUEST_RESEND_TOO_SOON))
                .given(careInvitationResendLimiter).acquire(invitation.requestKey());

        assertThatThrownBy(() ->
                careInvitationManager.resend(CareFixture.REQUEST_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_REQUEST_RESEND_TOO_SOON);

        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }
}
