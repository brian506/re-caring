package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
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

    @Mock
    private DesignatedAvatarManager designatedAvatarManager;

    @Test
    @DisplayName("보호 대상자 목록에는 회원 정보와 내 역할이 함께 담긴다")
    void findWardInfos_combines_member_and_care_role() {
        // given
        Member ward = CareFixture.createWardMember();
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createPrimaryGuardianRelationship(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)));
        given(memberReader.findAllByMemberKeys(List.of(CareFixture.WARD_MEMBER_KEY)))
                .willReturn(Map.of(CareFixture.WARD_MEMBER_KEY, ward));

        // when
        List<WardInfo> result = careRelationshipReader.findWardInfos(CareFixture.GUARDIAN_MEMBER_KEY);

        // then
        assertThat(result).containsExactly(
                CareFixture.createWardInfo(ward.getMemberKey(), CareRole.PRIMARY_GUARDIAN));
    }

    @Test
    @DisplayName("보호 대상자 목록에는 실명과 별명이 함께 담긴다")
    void findWardInfos_includes_both_real_name_and_nickname() {
        // given
        Member ward = CareFixture.createWardMember();
        CareRelationship relationship = CareFixture.createPrimaryGuardianRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
        relationship.changeWardNickname("할머니");
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(List.of(relationship));
        given(memberReader.findAllByMemberKeys(List.of(CareFixture.WARD_MEMBER_KEY)))
                .willReturn(Map.of(CareFixture.WARD_MEMBER_KEY, ward));

        // when
        List<WardInfo> result = careRelationshipReader.findWardInfos(CareFixture.GUARDIAN_MEMBER_KEY);

        // then
        assertThat(result).containsExactly(
                CareFixture.createWardInfo(ward.getMemberKey(), "할머니", CareRole.PRIMARY_GUARDIAN));
    }

    @Test
    @DisplayName("보호 대상자 목록 - 별명을 설정하지 않았으면 별명은 null이고 실명은 유지된다")
    void findWardInfos_returns_null_nickname_when_not_set() {
        // given
        Member ward = CareFixture.createWardMember();
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createPrimaryGuardianRelationship(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)));
        given(memberReader.findAllByMemberKeys(List.of(CareFixture.WARD_MEMBER_KEY)))
                .willReturn(Map.of(CareFixture.WARD_MEMBER_KEY, ward));

        // when
        List<WardInfo> result = careRelationshipReader.findWardInfos(CareFixture.GUARDIAN_MEMBER_KEY);

        // then
        assertThat(result).singleElement()
                .satisfies(info -> {
                    assertThat(info.wardNickname()).isNull();
                    assertThat(info.wardName()).isEqualTo("보호대상자");
                });
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


    @Test
    @DisplayName("보호 대상자 목록에는 대상자 본인이 고른 얼굴과 내가 지정한 얼굴이 함께 담긴다")
    void findWardInfos_includes_own_and_designated_avatar_codes() {
        // given
        Member ward = CareFixture.createWardMember();
        ward.changeProfileAvatarCode(CareFixture.SENIOR_AVATAR_CODE);
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createPrimaryGuardianRelationship(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)));
        given(memberReader.findAllByMemberKeys(List.of(CareFixture.WARD_MEMBER_KEY)))
                .willReturn(Map.of(CareFixture.WARD_MEMBER_KEY, ward));
        given(designatedAvatarManager.findWardAvatarCodes(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(Map.of(ward.getMemberKey(), CareFixture.OTHER_SENIOR_AVATAR_CODE));

        // when
        List<WardInfo> result = careRelationshipReader.findWardInfos(CareFixture.GUARDIAN_MEMBER_KEY);

        // then
        assertThat(result).containsExactly(CareFixture.createWardInfo(
                ward.getMemberKey(), null, CareRole.PRIMARY_GUARDIAN,
                CareFixture.SENIOR_AVATAR_CODE, CareFixture.OTHER_SENIOR_AVATAR_CODE));
    }

    @Test
    @DisplayName("보호자·관계자 목록에는 그 사람이 고른 얼굴과 내가 지정한 얼굴이 함께 담긴다")
    void findCaregiverInfos_includes_own_and_designated_avatar_codes() {
        // given
        Member manager = CareFixture.createGuardianMember();
        manager.changeProfileAvatarCode(CareFixture.ADULT_AVATAR_CODE);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createManagerRelationship(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY)));
        given(memberReader.findAllByMemberKeys(List.of(CareFixture.MANAGER_MEMBER_KEY)))
                .willReturn(Map.of(CareFixture.MANAGER_MEMBER_KEY, manager));
        given(designatedAvatarManager.findAvatarCodesInWard(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .willReturn(Map.of(manager.getMemberKey(), CareFixture.SENIOR_AVATAR_CODE));

        // when
        List<CaregiverInfo> result = careRelationshipReader.findCaregiverInfos(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);

        // then
        assertThat(result).containsExactly(CareFixture.createCaregiverInfo(
                manager.getMemberKey(), CareRole.MANAGER,
                CareFixture.ADULT_AVATAR_CODE, CareFixture.SENIOR_AVATAR_CODE));
    }

    @Test
    @DisplayName("보는 사람이 없는 알림 경로에서는 지정 얼굴을 조회하지 않고 비워 둔다")
    void findCaregiverInfos_without_requester_leaves_designated_avatar_empty() {
        // given
        Member manager = CareFixture.createGuardianMember();
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createManagerRelationship(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY)));
        given(memberReader.findAllByMemberKeys(List.of(CareFixture.MANAGER_MEMBER_KEY)))
                .willReturn(Map.of(CareFixture.MANAGER_MEMBER_KEY, manager));

        // when
        List<CaregiverInfo> result = careRelationshipReader.findCaregiverInfos(CareFixture.WARD_MEMBER_KEY);

        // then
        assertThat(result).singleElement()
                .satisfies(info -> assertThat(info.designatedProfileAvatarCode()).isNull());
        then(designatedAvatarManager).shouldHaveNoInteractions();
    }
}
