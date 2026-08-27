package com.recaring.member.implement;

import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.vo.NewLocalMember;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.member.fixture.MemberFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Captor
    private ArgumentCaptor<Member> memberCaptor;

    @Test
    @DisplayName("로컬 회원 가입은 입력받은 값을 각각 제자리에 담고 가입 유형 LOCAL로 저장한다")
    void registerLocalMember_persists_local_member_from_input() {
        // Given
        NewLocalMember input = AuthFixture.createNewLocalMember();
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        String memberKey = memberWriter.registerLocalMember(input);

        // Then
        then(memberRepository).should().save(memberCaptor.capture());
        Member saved = memberCaptor.getValue();
        assertThat(saved.getPhone()).isEqualTo(MemberFixture.PHONE);
        assertThat(saved.getName()).isEqualTo(MemberFixture.NAME);
        assertThat(saved.getBirth()).isEqualTo(MemberFixture.BIRTH);
        assertThat(saved.getGender()).isEqualTo(MemberFixture.GENDER);
        assertThat(saved.getRole()).isEqualTo(MemberFixture.ROLE);
        assertThat(saved.getSignUpType()).isEqualTo(SignUpType.LOCAL);
        assertThat(memberKey).isEqualTo(saved.getMemberKey());
    }
}
