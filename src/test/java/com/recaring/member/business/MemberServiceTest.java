package com.recaring.member.business;

import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.vo.Password;
import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.implement.local.LocalAuthManager;
import com.recaring.auth.implement.local.LocalAuthReader;
import com.recaring.member.controller.response.MyInfoResponse;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.implement.MemberWithdrawalManager;
import com.recaring.member.implement.MemberWriter;
import com.recaring.member.implement.MembersTermsAgreementReader;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.safezone.implement.SafeZoneReader;
import com.recaring.safezone.vo.SafeZoneInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberReader memberReader;

    @Mock
    private MemberWriter memberWriter;

    @Mock
    private MemberWithdrawalManager memberWithdrawalManager;

    @Mock
    private LocalAuthReader localAuthReader;

    @Mock
    private LocalAuthAuthenticator localAuthAuthenticator;

    @Mock
    private LocalAuthManager localAuthManager;

    @Mock
    private MembersTermsAgreementReader membersTermsAgreementReader;

    @Mock
    private SafeZoneReader safeZoneReader;

    @Test
    @DisplayName("내 정보 조회 성공 - 회원 정보, 이메일, 약관 동의 시각, 안심존 목록을 조합해 반환한다")
    void getMyInfo_success() {
        Member member = MemberFixture.createMember();
        MembersTermsAgreement terms = MemberFixture.createTermsAgreement();
        List<SafeZoneInfo> safeZones = List.of(SafeZoneFixture.createSafeZoneInfo("safe-zone-key-1"));

        given(memberReader.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(member);
        given(localAuthReader.findEmailByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(AuthFixture.EMAIL);
        given(membersTermsAgreementReader.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(terms);
        given(safeZoneReader.findAllByWardMemberKey(MemberFixture.MEMBER_KEY)).willReturn(safeZones);

        MyInfoResponse result = memberService.getMyInfo(MemberFixture.MEMBER_KEY);

        assertThat(result.name()).isEqualTo(MemberFixture.NAME);
        assertThat(result.phone()).isEqualTo(MemberFixture.PHONE);
        assertThat(result.email()).isEqualTo(AuthFixture.EMAIL);
        assertThat(result.termsServiceAgreedAt()).isNotNull();
        assertThat(result.termsPrivacyAgreedAt()).isNotNull();
        assertThat(result.termsLocationAgreedAt()).isNotNull();
        assertThat(result.safeZones()).hasSize(1);
        assertThat(result.safeZones().get(0).name()).isEqualTo(SafeZoneFixture.NAME);

        then(memberReader).should(times(1)).findByMemberKey(MemberFixture.MEMBER_KEY);
        then(localAuthReader).should(times(1)).findEmailByMemberKey(MemberFixture.MEMBER_KEY);
        then(membersTermsAgreementReader).should(times(1)).findByMemberKey(MemberFixture.MEMBER_KEY);
        then(safeZoneReader).should(times(1)).findAllByWardMemberKey(MemberFixture.MEMBER_KEY);
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


    @Test
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
    @DisplayName("내 정보 수정 성공 - 현재 비밀번호 확인 후 새 비밀번호로 인코딩·변경까지 위임한다")
    void updateMyInfo_success_with_password() {
        // given
        given(localAuthAuthenticator.encodePassword(any(Password.class)))
                .willReturn(AuthFixture.createEncodedPassword());

        // when
        memberService.updateMyInfo(
                MemberFixture.MEMBER_KEY,
                MemberFixture.UPDATED_NAME,
                MemberFixture.UPDATED_BIRTH,
                AuthFixture.RAW_PASSWORD,
                AuthFixture.RAW_PASSWORD
        );

        // then
        then(memberWriter).should(times(1))
                .updateProfile(MemberFixture.MEMBER_KEY, MemberFixture.UPDATED_NAME, MemberFixture.UPDATED_BIRTH);
        then(localAuthAuthenticator).should(times(1)).verifyPassword(eq(MemberFixture.MEMBER_KEY), any(Password.class));
        then(localAuthAuthenticator).should(times(1)).encodePassword(any(Password.class));
        then(localAuthManager).should(times(1))
                .changePassword(MemberFixture.MEMBER_KEY, AuthFixture.ENCODED_PASSWORD);
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 비밀번호가 비어 있으면 프로필만 수정하고 비밀번호 검증·변경은 수행하지 않는다")
    void updateMyInfo_success_without_password() {
        // when
        memberService.updateMyInfo(
                MemberFixture.MEMBER_KEY,
                MemberFixture.UPDATED_NAME,
                MemberFixture.UPDATED_BIRTH,
                null,
                null
        );

        // then
        then(memberWriter).should(times(1))
                .updateProfile(MemberFixture.MEMBER_KEY, MemberFixture.UPDATED_NAME, MemberFixture.UPDATED_BIRTH);
        then(localAuthAuthenticator).should(never()).verifyPassword(any(), any());
        then(localAuthAuthenticator).should(never()).encodePassword(any(Password.class));
        then(localAuthManager).should(never()).changePassword(any(), any());
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 현재 비밀번호가 일치하지 않으면 예외가 발생하고 비밀번호는 변경되지 않는다")
    void updateMyInfo_throws_when_current_password_mismatch() {
        // given
        willThrow(new AppException(ErrorType.INVALID_PASSWORD))
                .given(localAuthAuthenticator).verifyPassword(eq(MemberFixture.MEMBER_KEY), any(Password.class));

        // when & then
        assertThatThrownBy(() -> memberService.updateMyInfo(
                MemberFixture.MEMBER_KEY,
                MemberFixture.UPDATED_NAME,
                MemberFixture.UPDATED_BIRTH,
                "wrongpassword1",
                AuthFixture.RAW_PASSWORD
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD);

        then(localAuthAuthenticator).should(never()).encodePassword(any(Password.class));
        then(localAuthManager).should(never()).changePassword(any(), any());
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