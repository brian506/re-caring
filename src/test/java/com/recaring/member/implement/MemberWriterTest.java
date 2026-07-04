package com.recaring.member.implement;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberWriter 단위 테스트")
class MemberWriterTest {

    @InjectMocks
    private MemberWriter memberWriter;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("프로필 수정 성공 - 이름과 생년월일이 모두 갱신된다")
    void updateProfile_success() {
        // given
        Member member = MemberFixture.createMember();
        given(memberRepository.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(Optional.of(member));

        // when
        memberWriter.updateProfile(MemberFixture.MEMBER_KEY, MemberFixture.UPDATED_NAME, MemberFixture.UPDATED_BIRTH);

        // then
        assertThat(member.getName()).isEqualTo(MemberFixture.UPDATED_NAME);
        assertThat(member.getBirth()).isEqualTo(MemberFixture.UPDATED_BIRTH);
    }

    @Test
    @DisplayName("프로필 부분 수정 - null 값은 기존 값을 유지한다")
    void updateProfile_partial_skips_null() {
        // given
        Member member = MemberFixture.createMember();
        given(memberRepository.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(Optional.of(member));

        // when
        memberWriter.updateProfile(MemberFixture.MEMBER_KEY, MemberFixture.UPDATED_NAME, null);

        // then
        assertThat(member.getName()).isEqualTo(MemberFixture.UPDATED_NAME);
        assertThat(member.getBirth()).isEqualTo(MemberFixture.BIRTH);
    }

    @Test
    @DisplayName("프로필 수정 실패 - 존재하지 않는 회원이면 AppException이 발생한다")
    void updateProfile_throws_when_member_not_found() {
        // given
        given(memberRepository.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberWriter.updateProfile(
                MemberFixture.MEMBER_KEY, MemberFixture.UPDATED_NAME, MemberFixture.UPDATED_BIRTH))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }
}
