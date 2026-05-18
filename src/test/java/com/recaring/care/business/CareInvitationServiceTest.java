package com.recaring.care.business;

import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareInvitationManager;
import com.recaring.care.implement.CareInvitationReader;
import com.recaring.care.vo.ReceivedRequestInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CareInvitationService 단위 테스트")
class CareInvitationServiceTest {

    @InjectMocks
    private CareInvitationService careInvitationService;

    @Mock
    private CareInvitationManager careInvitationManager;

    @Mock
    private CareInvitationReader careInvitationReader;

    @Test
    @DisplayName("보호 대상자 초대 요청 시 Manager에 위임한다")
    void sendWardInvitation_delegates_to_manager() {
        careInvitationService.sendWardInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_PHONE);

        then(careInvitationManager).should(times(1))
                .sendWardInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_PHONE);
    }

    @Test
    @DisplayName("관리자 초대 요청 시 Manager에 위임한다")
    void sendManagerInvitation_delegates_to_manager() {
        careInvitationService.sendManagerInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.MANAGER_PHONE, CareFixture.WARD_MEMBER_KEY);

        then(careInvitationManager).should(times(1))
                .sendManagerInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.MANAGER_PHONE, CareFixture.WARD_MEMBER_KEY);
    }

    @Test
    @DisplayName("보호자 초대 요청 시 Manager에 위임한다")
    void sendGuardianInvitation_delegates_to_manager() {
        careInvitationService.sendGuardianInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.MANAGER_PHONE, CareFixture.WARD_MEMBER_KEY);

        then(careInvitationManager).should(times(1))
                .sendGuardianInvitation(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.MANAGER_PHONE, CareFixture.WARD_MEMBER_KEY);
    }

    @Test
    @DisplayName("받은 요청 목록 조회 시 Reader에서 조립된 결과를 반환한다")
    void getReceivedRequests_returns_reader_result() {
        List<ReceivedRequestInfo> expected = List.of();
        given(careInvitationReader.findReceivedRequestInfos(CareFixture.GUARDIAN_MEMBER_KEY)).willReturn(expected);

        List<ReceivedRequestInfo> result = careInvitationService.getReceivedRequests(CareFixture.GUARDIAN_MEMBER_KEY);

        assertThat(result).isEqualTo(expected);
        then(careInvitationReader).should(times(1)).findReceivedRequestInfos(CareFixture.GUARDIAN_MEMBER_KEY);
    }

    @Test
    @DisplayName("요청 수락 시 Manager에 위임한다")
    void accept_delegates_to_manager() {
        careInvitationService.accept(CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY);

        then(careInvitationManager).should(times(1))
                .accept(CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY);
    }

    @Test
    @DisplayName("요청 거절 시 Manager에 위임한다")
    void reject_delegates_to_manager() {
        careInvitationService.reject(CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY);

        then(careInvitationManager).should(times(1))
                .reject(CareFixture.REQUEST_KEY, CareFixture.WARD_MEMBER_KEY);
    }
}
