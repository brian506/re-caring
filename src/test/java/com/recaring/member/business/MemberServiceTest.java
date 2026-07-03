package com.recaring.member.business;

import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.vo.Password;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.implement.MemberWithdrawalManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberReader memberReader;

    @Mock
    private MemberWithdrawalManager memberWithdrawalManager;

    @Test
    @DisplayName("회원 탈퇴 성공 - memberKey와 비밀번호를 그대로 Manager에 위임한다")
    void withdraw_success() {
        // given
        String memberKey = AuthFixture.MEMBER_KEY;
        Password password = AuthFixture.createPassword();

        // when
        memberService.withdraw(memberKey, password);

        // then
        then(memberWithdrawalManager).should(times(1)).withdraw(memberKey, password);
    }
}
