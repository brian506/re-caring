package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.event.CareInvitationAcceptedEvent;
import com.recaring.care.event.CareInvitationSentEvent;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.vo.CareRelationshipRegistration;
import com.recaring.member.dataaccess.entity.Member;
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
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("보호 대상자 초대 - 전화번호로 WARD 회원을 찾아 초대장을 등록한다")
    void sendWardInvitation_success() {
        Member ward = CareFixture.createWardMember();
        CareInvitation saved = CareFixture.createWardInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        given(memberReader.findByPhone(new PhoneNumber(CareFixture.WARD_PHONE))).willReturn(ward);
        given(careInvitationWriter.register(any())).willReturn(saved);

        careInvitationManager.sendWardInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_PHONE);

        then(careRelationshipValidator).should(times(1))
                .validateCanAddWard(eq(CareFixture.GUARDIAN_MEMBER_KEY), eq(ward.getMemberKey()));
        then(careInvitationWriter).should(times(1)).register(any());
        then(eventPublisher).should(times(1)).publishEvent(any(CareInvitationSentEvent.class));
    }

    @Test
    @DisplayName("보호 대상자 초대 - WARD 역할이 아닌 회원을 추가하면 예외가 발생한다")
    void sendWardInvitation_fails_when_not_ward_role() {
        Member guardian = CareFixture.createGuardianMember(CareFixture.WARD_PHONE);
        given(memberReader.findByPhone(new PhoneNumber(CareFixture.WARD_PHONE))).willReturn(guardian);

        assertThatThrownBy(() ->
                careInvitationManager.sendWardInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_PHONE))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_WARD_ROLE);

        then(careInvitationWriter).should(times(0)).register(any());
    }

    @Test
    @DisplayName("관리자 초대 - 전화번호로 GUARDIAN 회원을 찾아 초대장을 등록한다")
    void sendManagerInvitation_success() {
        Member manager = CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE);
        CareInvitation saved = CareFixture.createManagerInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        given(memberReader.findByPhone(new PhoneNumber(CareFixture.MANAGER_PHONE))).willReturn(manager);
        given(careInvitationWriter.register(any())).willReturn(saved);

        careInvitationManager.sendManagerInvitation(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.MANAGER_PHONE, CareFixture.WARD_MEMBER_KEY);

        then(careRelationshipValidator).should(times(1))
                .validateCanAddManager(
                        eq(CareFixture.GUARDIAN_MEMBER_KEY),
                        eq(CareFixture.WARD_MEMBER_KEY),
                        eq(manager.getMemberKey()));
        then(careInvitationWriter).should(times(1)).register(any());
        then(eventPublisher).should(times(1)).publishEvent(any(CareInvitationSentEvent.class));
    }

    @Test
    @DisplayName("관리자 초대 - GUARDIAN 역할이 아닌 회원을 추가하면 예외가 발생한다")
    void sendManagerInvitation_fails_when_not_guardian_role() {
        Member ward = CareFixture.createWardMember(CareFixture.MANAGER_PHONE);
        given(memberReader.findByPhone(new PhoneNumber(CareFixture.MANAGER_PHONE))).willReturn(ward);

        assertThatThrownBy(() ->
                careInvitationManager.sendManagerInvitation(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.MANAGER_PHONE, CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_CAREGIVER_ROLE);

        then(careInvitationWriter).should(times(0)).register(any());
    }

    @Test
    @DisplayName("초대 수락 - 케어 관계를 생성하고 초대장 상태를 수락으로 변경하며 이벤트를 발행한다")
    void accept_creates_relationship_and_updates_status() {
        CareInvitation invitation = CareFixture.createWardInvitation(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        given(careInvitationReader.findByRequestKeyAndMemberKey(
                CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY)).willReturn(invitation);

        careInvitationManager.accept(CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY);

        then(careRelationshipWriter).should(times(1)).register(any(CareRelationshipRegistration.class), eq(CareFixture.WARD_MEMBER_KEY));
        then(careInvitationWriter).should(times(1)).accept(anyString());
        then(eventPublisher).should(times(1)).publishEvent(any(CareInvitationAcceptedEvent.class));
    }

    @Test
    @DisplayName("초대 거절 - 초대장 상태를 거절로 변경한다")
    void reject_updates_status_to_rejected() {
        CareInvitation invitation = CareFixture.createWardInvitation(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        given(careInvitationReader.findByRequestKeyAndMemberKey(
                CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY)).willReturn(invitation);

        careInvitationManager.reject(CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY);

        then(careInvitationWriter).should(times(1)).reject(anyString());
        then(careRelationshipWriter).should(times(0)).register(any(), any());
    }
}
