package com.recaring.member.implement;

import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.vo.NewLocalMember;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.member.dataaccess.entity.SubscriptionType;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberWriter 단위 테스트")
class MemberWriterTest {

    @InjectMocks
    private MemberWriter memberWriter;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("로컬 회원 가입은 입력값을 그대로 담고 가입 유형 LOCAL·구독 BASIC으로 저장한다")
    void registerLocalMember_persists_local_member_from_input() {
        NewLocalMember input = NewLocalMember.builder()
                .email(AuthFixture.createLocalEmail())
                .password(AuthFixture.createEncodedPassword())
                .phone(new PhoneNumber(MemberFixture.PHONE))
                .name(MemberFixture.NAME)
                .birth(MemberFixture.BIRTH)
                .gender(MemberFixture.GENDER)
                .role(MemberFixture.ROLE)
                .build();
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        String memberKey = memberWriter.registerLocalMember(input);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        then(memberRepository).should().save(captor.capture());
        Member saved = captor.getValue();
        // SPEC snapshot.md 엔티티 Member: memberKey(UUID), role, name, phone
        assertThat(saved.getPhone()).isEqualTo(MemberFixture.PHONE);
        assertThat(saved.getName()).isEqualTo(MemberFixture.NAME);
        assertThat(saved.getBirth()).isEqualTo(MemberFixture.BIRTH);
        assertThat(saved.getGender()).isEqualTo(MemberFixture.GENDER);
        assertThat(saved.getRole()).isEqualTo(MemberFixture.ROLE);
        // SPEC POST /api/v1/auth/sign-up 로컬 회원가입 → SignUpType.LOCAL
        assertThat(saved.getSignUpType()).isEqualTo(SignUpType.LOCAL);
        // SPEC audit A-10: 신규 회원은 BASIC으로 시작
        assertThat(saved.getSubscriptionType()).isEqualTo(SubscriptionType.BASIC);
        // SPEC architecture.md 외부 식별자: 반환값은 저장된 회원의 memberKey
        assertThat(memberKey).isEqualTo(saved.getMemberKey());
    }

    @Test
    @DisplayName("프로필 수정은 조회한 회원 객체의 이름·생년월일을 바꾼다")
    void updateProfile_mutates_loaded_member() {
        Member member = MemberFixture.createMember();
        given(memberRepository.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(Optional.of(member));

        memberWriter.updateProfile(MemberFixture.MEMBER_KEY, MemberFixture.UPDATED_NAME, MemberFixture.UPDATED_BIRTH);

        // SPEC PATCH /api/v1/members/me: 이름·생년월일 부분 수정
        assertThat(member.getName()).isEqualTo(MemberFixture.UPDATED_NAME);
        assertThat(member.getBirth()).isEqualTo(MemberFixture.UPDATED_BIRTH);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 프로필을 수정하면 NOT_FOUND_ACCOUNT 예외가 발생한다")
    void updateProfile_throws_when_member_absent() {
        given(memberRepository.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(Optional.empty());

        // SPEC ErrorType.NOT_FOUND_ACCOUNT(E2016)
        assertThatThrownBy(() -> memberWriter.updateProfile(
                MemberFixture.MEMBER_KEY, MemberFixture.UPDATED_NAME, MemberFixture.UPDATED_BIRTH))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }
}
