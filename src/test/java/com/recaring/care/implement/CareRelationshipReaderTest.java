package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.care.vo.WardInfo;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CareRelationshipReader 단위 테스트")
class CareRelationshipReaderTest {

    @InjectMocks
    private CareRelationshipReader careRelationshipReader;

    @Mock
    private CareRelationshipRepository careRelationshipRepository;

    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("보호 대상자 목록에는 회원 정보와 내 역할이 함께 담긴다")
    void findWardInfos_combines_member_and_care_role() {
        // given
        Member ward = CareFixture.createWardMember();
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createGuardianRelationship(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)));
        given(memberReader.findAllByMemberKeys(List.of(CareFixture.WARD_MEMBER_KEY)))
                .willReturn(Map.of(CareFixture.WARD_MEMBER_KEY, ward));

        // when
        List<WardInfo> result = careRelationshipReader.findWardInfos(CareFixture.GUARDIAN_MEMBER_KEY);

        // then
        assertThat(result).containsExactly(
                CareFixture.createWardInfo(ward.getMemberKey(), CareRole.GUARDIAN));
    }

    @Test
    @DisplayName("보호 대상자가 없으면 빈 목록을 반환한다")
    void findWardInfos_returns_empty_when_no_relationship() {
        // given
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(List.of());
        given(memberReader.findAllByMemberKeys(List.of())).willReturn(Map.of());

        // when
        List<WardInfo> result = careRelationshipReader.findWardInfos(CareFixture.GUARDIAN_MEMBER_KEY);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("보호자·관계자 목록에는 회원 정보와 케어 역할이 함께 담긴다")
    void findCaregiverInfos_combines_member_and_care_role() {
        // given
        Member caregiver = CareFixture.createGuardianMember();
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createManagerRelationship(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY)));
        given(memberReader.findAllByMemberKeys(List.of(CareFixture.MANAGER_MEMBER_KEY)))
                .willReturn(Map.of(CareFixture.MANAGER_MEMBER_KEY, caregiver));

        // when
        List<CaregiverInfo> result = careRelationshipReader.findCaregiverInfos(CareFixture.WARD_MEMBER_KEY);

        // then
        assertThat(result).containsExactly(
                CareFixture.createCaregiverInfo(caregiver.getMemberKey(), CareRole.MANAGER));
    }

    @Test
    @DisplayName("보호 대상자와 보호자 사이의 케어 역할을 반환한다")
    void findCareRole_returns_role() {
        // given
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY)
                ));

        // when
        CareRole result = careRelationshipReader.findCareRole(
                CareFixture.WARD_MEMBER_KEY,
                CareFixture.MANAGER_MEMBER_KEY
        );

        // then
        assertThat(result).isEqualTo(CareRole.MANAGER);
        then(careRelationshipRepository).should(times(1)).findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY);
    }

    @Test
    @DisplayName("케어 관계가 없는 보호 대상자의 역할을 조회하면 예외가 발생한다")
    void findCareRole_throws_exception_when_not_related() {
        // given
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)
                ));

        // when / then
        assertThatThrownBy(() -> careRelationshipReader.findCareRole(
                CareFixture.WARD_MEMBER_KEY,
                CareFixture.MANAGER_MEMBER_KEY
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_CARE_RELATED_WARD);
    }

}
