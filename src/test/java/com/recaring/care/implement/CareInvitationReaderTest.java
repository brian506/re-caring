package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareInvitationRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.vo.ReceivedRequestInfo;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CareInvitationReader 단위 테스트")
class CareInvitationReaderTest {

    @InjectMocks
    private CareInvitationReader careInvitationReader;

    @Mock
    private CareInvitationRepository careInvitationRepository;

    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("받은 요청 목록에는 요청자와 보호 대상자 정보가 함께 담긴다")
    void findReceivedRequestInfos_combines_requester_and_ward() {
        // given
        Member requester = CareFixture.createGuardianMember();
        Member ward = CareFixture.createWardMember();
        CareInvitation invitation = CareFixture.createManagerInvitation(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        given(careInvitationRepository.findReceivedPendingRequests(CareFixture.MANAGER_MEMBER_KEY))
                .willReturn(List.of(invitation));
        given(memberReader.findAllByMemberKeys(List.of(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY)))
                .willReturn(Map.of(
                        CareFixture.GUARDIAN_MEMBER_KEY, requester,
                        CareFixture.WARD_MEMBER_KEY, ward
                ));

        // when
        List<ReceivedRequestInfo> result = careInvitationReader.findReceivedRequestInfos(CareFixture.MANAGER_MEMBER_KEY);

        // then
        assertThat(result).containsExactly(CareFixture.createReceivedRequestInfo(
                invitation.getRequestKey(),
                requester.getMemberKey(),
                ward.getMemberKey(),
                CareRole.MANAGER,
                invitation.getCreatedAt()
        ));
    }

    @Test
    @DisplayName("요청자가 곧 보호 대상자인 요청은 회원 조회 키를 중복 없이 한 번만 넘긴다")
    void findReceivedRequestInfos_deduplicates_member_keys() {
        // given
        Member ward = CareFixture.createWardMember();
        CareInvitation invitation = CareFixture.createWardInvitation(
                CareFixture.WARD_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        given(careInvitationRepository.findReceivedPendingRequests(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(invitation));
        given(memberReader.findAllByMemberKeys(List.of(CareFixture.WARD_MEMBER_KEY)))
                .willReturn(Map.of(CareFixture.WARD_MEMBER_KEY, ward));

        // when
        careInvitationReader.findReceivedRequestInfos(CareFixture.WARD_MEMBER_KEY);

        // then
        then(memberReader).should(times(1)).findAllByMemberKeys(List.of(CareFixture.WARD_MEMBER_KEY));
    }

    @Test
    @DisplayName("받은 요청이 없으면 빈 목록을 반환한다")
    void findReceivedRequestInfos_returns_empty_when_no_requests() {
        // given
        given(careInvitationRepository.findReceivedPendingRequests(CareFixture.MANAGER_MEMBER_KEY))
                .willReturn(List.of());
        given(memberReader.findAllByMemberKeys(List.of())).willReturn(Map.of());

        // when
        List<ReceivedRequestInfo> result = careInvitationReader.findReceivedRequestInfos(CareFixture.MANAGER_MEMBER_KEY);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("수신자에게 온 요청이 아니면 예외가 발생한다")
    void findInvitationForRecipient_throws_when_not_found() {
        // given
        given(careInvitationRepository.findInvitationForRecipient(
                CareFixture.REQUEST_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> careInvitationReader.findInvitationForRecipient(
                CareFixture.REQUEST_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_REQUEST);
    }
}
