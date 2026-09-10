package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.DesignatedAvatar;
import com.recaring.care.dataaccess.repository.DesignatedAvatarRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.member.vo.ProfileAvatarCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("DesignatedAvatarManager 단위 테스트")
class DesignatedAvatarManagerTest {

    private static final String OWNER_KEY = CareFixture.GUARDIAN_MEMBER_KEY;
    private static final String WARD_KEY = CareFixture.WARD_MEMBER_KEY;
    private static final String MANAGER_KEY = CareFixture.MANAGER_MEMBER_KEY;

    @InjectMocks
    private DesignatedAvatarManager designatedAvatarManager;

    @Mock
    private DesignatedAvatarRepository designatedAvatarRepository;

    @Captor
    private ArgumentCaptor<DesignatedAvatar> avatarCaptor;

    @Test
    @DisplayName("지정한 얼굴이 없으면 요청자·대상자·대상 범위로 새 행을 저장한다")
    void designate_saves_new_row_when_absent() {
        given(designatedAvatarRepository.findByOwnerMemberKeyAndWardMemberKeyAndTargetMemberKey(OWNER_KEY, WARD_KEY, WARD_KEY))
                .willReturn(Optional.empty());

        designatedAvatarManager.designate(OWNER_KEY, WARD_KEY, WARD_KEY,
                new ProfileAvatarCode(CareFixture.SENIOR_AVATAR_CODE));

        then(designatedAvatarRepository).should().save(avatarCaptor.capture());
        assertThat(avatarCaptor.getValue())
                .extracting(DesignatedAvatar::getOwnerMemberKey, DesignatedAvatar::getWardMemberKey,
                        DesignatedAvatar::getTargetMemberKey, DesignatedAvatar::getProfileAvatarCode)
                .containsExactly(OWNER_KEY, WARD_KEY, WARD_KEY, CareFixture.SENIOR_AVATAR_CODE);
    }

    @Test
    @DisplayName("이미 지정한 얼굴이 있으면 새 행을 만들지 않고 코드만 바꾼다")
    void designate_replaces_code_when_present() {
        DesignatedAvatar existing = CareFixture.createDesignatedAvatar(
                OWNER_KEY, WARD_KEY, WARD_KEY, CareFixture.SENIOR_AVATAR_CODE);
        given(designatedAvatarRepository.findByOwnerMemberKeyAndWardMemberKeyAndTargetMemberKey(OWNER_KEY, WARD_KEY, WARD_KEY))
                .willReturn(Optional.of(existing));

        designatedAvatarManager.designate(OWNER_KEY, WARD_KEY, WARD_KEY,
                new ProfileAvatarCode(CareFixture.OTHER_SENIOR_AVATAR_CODE));

        assertThat(existing.getProfileAvatarCode()).isEqualTo(CareFixture.OTHER_SENIOR_AVATAR_CODE);
        then(designatedAvatarRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("해제를 보냈는데 지정한 얼굴이 있으면 그 행을 삭제한다")
    void designate_deletes_row_when_cleared_and_present() {
        DesignatedAvatar existing = CareFixture.createDesignatedAvatar(
                OWNER_KEY, WARD_KEY, WARD_KEY, CareFixture.SENIOR_AVATAR_CODE);
        given(designatedAvatarRepository.findByOwnerMemberKeyAndWardMemberKeyAndTargetMemberKey(OWNER_KEY, WARD_KEY, WARD_KEY))
                .willReturn(Optional.of(existing));

        designatedAvatarManager.designate(OWNER_KEY, WARD_KEY, WARD_KEY, ProfileAvatarCode.from(""));

        then(designatedAvatarRepository).should().delete(existing);
        then(designatedAvatarRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("해제를 보냈는데 지정한 얼굴이 없으면 아무것도 저장하거나 지우지 않는다")
    void designate_does_nothing_when_cleared_and_absent() {
        given(designatedAvatarRepository.findByOwnerMemberKeyAndWardMemberKeyAndTargetMemberKey(OWNER_KEY, WARD_KEY, WARD_KEY))
                .willReturn(Optional.empty());

        designatedAvatarManager.designate(OWNER_KEY, WARD_KEY, WARD_KEY, ProfileAvatarCode.from(""));

        then(designatedAvatarRepository).should(never()).save(any());
        then(designatedAvatarRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("대상자 목록용 조회는 대상자 본인에게 붙인 얼굴만 담고 보호자에게 붙인 얼굴은 제외한다")
    void findWardAvatarCodes_keeps_only_ward_self_designations() {
        given(designatedAvatarRepository.findAllByOwnerMemberKey(OWNER_KEY)).willReturn(List.of(
                CareFixture.createDesignatedAvatar(OWNER_KEY, WARD_KEY, WARD_KEY, CareFixture.SENIOR_AVATAR_CODE),
                CareFixture.createDesignatedAvatar(OWNER_KEY, WARD_KEY, MANAGER_KEY, CareFixture.ADULT_AVATAR_CODE)));

        Map<String, String> result = designatedAvatarManager.findWardAvatarCodes(OWNER_KEY);

        assertThat(result).containsExactly(Map.entry(WARD_KEY, CareFixture.SENIOR_AVATAR_CODE));
    }
}
