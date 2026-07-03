package com.recaring.member.business;

import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.vo.Password;
import com.recaring.auth.implement.local.LocalAuthReader;
import com.recaring.member.controller.response.MyInfoResponse;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.implement.MemberWithdrawalManager;
import com.recaring.member.implement.MembersTermsAgreementReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
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
    private LocalAuthReader localAuthReader;

    @Mock
    private MembersTermsAgreementReader membersTermsAgreementReader;

    @Test
    @DisplayName("내 정보 조회 성공 - 회원 정보, 이메일, 약관 동의 시각을 조합해 반환한다")
    void getMyInfo_success() {
        Member member = MemberFixture.createMember();
        MembersTermsAgreement terms = MemberFixture.createTermsAgreement();

        given(memberReader.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(member);
        given(localAuthReader.findEmailByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(AuthFixture.EMAIL);
        given(membersTermsAgreementReader.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(terms);

        MyInfoResponse result = memberService.getMyInfo(MemberFixture.MEMBER_KEY);

        assertThat(result.name()).isEqualTo(MemberFixture.NAME);
        assertThat(result.phone()).isEqualTo(MemberFixture.PHONE);
        assertThat(result.email()).isEqualTo(AuthFixture.EMAIL);
        assertThat(result.termsServiceAgreedAt()).isNotNull();
        assertThat(result.termsPrivacyAgreedAt()).isNotNull();
        assertThat(result.termsLocationAgreedAt()).isNotNull();

        then(memberReader).should(times(1)).findByMemberKey(MemberFixture.MEMBER_KEY);
        then(localAuthReader).should(times(1)).findEmailByMemberKey(MemberFixture.MEMBER_KEY);
        then(membersTermsAgreementReader).should(times(1)).findByMemberKey(MemberFixture.MEMBER_KEY);
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회 시 예외가 전파된다")
    void getMyInfo_throws_when_member_not_found() {
        willThrow(new AppException(ErrorType.NOT_FOUND_ACCOUNT))
                .given(memberReader).findByMemberKey(MemberFixture.MEMBER_KEY);

        assertThatThrownBy(() -> memberService.getMyInfo(MemberFixture.MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);

        then(localAuthReader).should(times(0)).findEmailByMemberKey(MemberFixture.MEMBER_KEY);
        then(membersTermsAgreementReader).should(times(0)).findByMemberKey(MemberFixture.MEMBER_KEY);
    }


    @DisplayName("이메일 조회 실패 시 예외가 전파된다")
    void getMyInfo_throws_when_email_not_found() {
        Member member = MemberFixture.createMember();
        given(memberReader.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(member);
        willThrow(new AppException(ErrorType.NOT_FOUND_ACCOUNT))
                .given(localAuthReader).findEmailByMemberKey(MemberFixture.MEMBER_KEY);

        assertThatThrownBy(() -> memberService.getMyInfo(MemberFixture.MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
        then(membersTermsAgreementReader).should(times(0)).findByMemberKey(MemberFixture.MEMBER_KEY);
    }

    @Test
    @DisplayName("약관 동의 정보 조회 실패 시 예외가 전파된다")
    void getMyInfo_throws_when_terms_not_found() {
        Member member = MemberFixture.createMember();
        given(memberReader.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(member);
        given(localAuthReader.findEmailByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(AuthFixture.EMAIL);
        willThrow(new AppException(ErrorType.NOT_FOUND_ACCOUNT))
                .given(membersTermsAgreementReader).findByMemberKey(MemberFixture.MEMBER_KEY);

        assertThatThrownBy(() -> memberService.getMyInfo(MemberFixture.MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }

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