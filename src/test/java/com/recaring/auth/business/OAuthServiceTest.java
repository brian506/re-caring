package com.recaring.auth.business;

import com.recaring.auth.business.command.OAuthSignUpCommand;
import com.recaring.auth.controller.response.OAuthSignInResponse;
import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.implement.TokenIssuer;
import com.recaring.auth.implement.oauth.OAuthAuthenticator;
import com.recaring.auth.implement.oauth.OAuthManager;
import com.recaring.auth.implement.oauth.OAuthReader;
import com.recaring.auth.vo.NewOauthMember;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;
import com.recaring.domain.member.dataaccess.entity.Gender;
import com.recaring.domain.member.dataaccess.entity.Member;
import com.recaring.domain.member.dataaccess.entity.MemberRole;
import com.recaring.domain.member.fixture.MemberFixture;
import com.recaring.domain.member.implement.MemberReader;
import com.recaring.security.vo.Jwt;
import com.recaring.sms.fixture.SmsFixture;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthService 단위 테스트")
class OAuthServiceTest {

    private OAuthService oAuthService;

    @Mock
    private OAuthReader oAuthReader;

    @Mock
    private OAuthManager oAuthManager;

    @Mock
    private MemberReader memberReader;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private PhoneVerificationReader phoneVerificationReader;

    @Mock
    private OAuthAuthenticator kakaoAuthenticator;

    @Mock
    private OAuthAuthenticator naverAuthenticator;

    @BeforeEach
    void setUp() {
        oAuthService = new OAuthService(
                List.of(kakaoAuthenticator, naverAuthenticator),
                oAuthReader,
                oAuthManager,
                memberReader,
                tokenIssuer,
                phoneVerificationReader
        );
    }

    @Test
    @DisplayName("OAuth 로그인 성공 - 기존 사용자 로그인")
    void signIn_success_existing_user() {
        // given
        String accessToken = "kakao-access-token";
        OAuthProvider provider = OAuthProvider.KAKAO;
        String providerMemberId = "kakao-user-123";
        String memberKey = "member-key-oauth";

        OAuthUser oAuthUser = new OAuthUser(providerMemberId, provider, "user@example.com", "카카오사용자");
        OAuth oAuth = OAuth.builder()
                .memberKey(memberKey)
                .provider(provider)
                .providerMemberId(providerMemberId)
                .build();
        Member member = MemberFixture.createMember();
        Jwt jwt = AuthFixture.createJwt();

        given(kakaoAuthenticator.supports(provider)).willReturn(true);
        given(kakaoAuthenticator.authentication(accessToken)).willReturn(oAuthUser);
        given(oAuthReader.findOAuthUser(provider, providerMemberId)).willReturn(Optional.of(oAuth));
        given(memberReader.findByMemberKey(memberKey)).willReturn(member);
        given(tokenIssuer.issue(member)).willReturn(jwt);

        // when
        OAuthSignInResponse result = oAuthService.signIn(accessToken, provider);

        // then
        assertThat(result.status()).isEqualTo(OAuthSignInResponse.SUCCESS);
        assertThat(result.accessToken()).isEqualTo(jwt.accessToken());
        assertThat(result.refreshToken()).isEqualTo(jwt.refreshToken());
        assertThat(result.providerMemberId()).isNull();
    }

    @Test
    @DisplayName("OAuth 로그인 응답 - 신규 사용자 회원가입 필요")
    void signIn_success_new_user_need_signup() {
        // given
        String accessToken = "naver-access-token";
        OAuthProvider provider = OAuthProvider.NAVER;
        String providerMemberId = "naver-user-456";

        OAuthUser oAuthUser = new OAuthUser(providerMemberId, provider, "newuser@example.com", "네이버사용자");

        given(naverAuthenticator.supports(provider)).willReturn(true);
        given(naverAuthenticator.authentication(accessToken)).willReturn(oAuthUser);
        given(oAuthReader.findOAuthUser(provider, providerMemberId)).willReturn(Optional.empty());

        // when
        OAuthSignInResponse result = oAuthService.signIn(accessToken, provider);

        // then
        assertThat(result.status()).isEqualTo(OAuthSignInResponse.NEED_SIGN_UP);
        assertThat(result.accessToken()).isNull();
        assertThat(result.refreshToken()).isNull();
        assertThat(result.providerMemberId()).isEqualTo(providerMemberId);
    }

    @Test
    @DisplayName("OAuth 회원가입 성공 - 신규 회원 등록 및 JWT 발급")
    void signUp_success() {
        // given
        String providerMemberId = "kakao-new-user";
        String smsToken = "sms-token-123";
        OAuthProvider provider = OAuthProvider.KAKAO;
        String memberKey = "member-new-oauth";

        OAuthSignUpCommand command = new OAuthSignUpCommand(
                providerMemberId,
                smsToken,
                "새카카오사용자",
                LocalDate.of(1999, 1, 15),
                Gender.FEMALE,
                MemberRole.GUARDIAN
        );

        PhoneNumber phone = SmsFixture.createPhoneNumber();
        Member member = MemberFixture.createMember();
        Jwt jwt = AuthFixture.createJwt();

        given(phoneVerificationReader.findPhoneByToken(smsToken)).willReturn(phone);
        given(oAuthManager.register(any(NewOauthMember.class))).willReturn(memberKey);
        given(memberReader.findByMemberKey(memberKey)).willReturn(member);
        given(tokenIssuer.issue(member)).willReturn(jwt);

        // when
        Jwt result = oAuthService.signUp(provider, command);

        // then
        assertThat(result.accessToken()).isEqualTo(jwt.accessToken());
        assertThat(result.refreshToken()).isEqualTo(jwt.refreshToken());
        then(oAuthManager).should(times(1)).register(any(NewOauthMember.class));
        then(tokenIssuer).should(times(1)).issue(member);
    }

    @Test
    @DisplayName("OAuth 로그인 실패 - 지원하지 않는 provider")
    void signIn_fail_unsupported_provider() {
        // given
        String accessToken = "some-token";
        OAuthProvider provider = OAuthProvider.KAKAO;

        given(kakaoAuthenticator.supports(provider)).willReturn(false);
        given(naverAuthenticator.supports(provider)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> oAuthService.signIn(accessToken, provider))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_OAUTH_USER);
    }

    @Test
    @DisplayName("OAuth 회원가입 - 카카오 provider")
    void signUp_with_kakao() {
        // given
        String providerMemberId = "kakao-123456";
        String smsToken = "token-kakao";
        OAuthProvider provider = OAuthProvider.KAKAO;
        String memberKey = "kakao-member";

        OAuthSignUpCommand command = new OAuthSignUpCommand(
                providerMemberId,
                smsToken,
                "카카오사용자",
                LocalDate.of(1993, 6, 20),
                Gender.MALE,
                MemberRole.GUARDIAN
        );

        PhoneNumber phone = SmsFixture.createPhoneNumber();
        Member member = MemberFixture.createMember();
        Jwt jwt = new Jwt("kakao-access", "kakao-refresh");

        given(phoneVerificationReader.findPhoneByToken(smsToken)).willReturn(phone);
        given(oAuthManager.register(any(NewOauthMember.class))).willReturn(memberKey);
        given(memberReader.findByMemberKey(memberKey)).willReturn(member);
        given(tokenIssuer.issue(member)).willReturn(jwt);

        // when
        Jwt result = oAuthService.signUp(provider, command);

        // then
        assertThat(result.accessToken()).isEqualTo("kakao-access");
    }

    @Test
    @DisplayName("OAuth 회원가입 - 네이버 provider")
    void signUp_with_naver() {
        // given
        String providerMemberId = "naver-987654";
        String smsToken = "token-naver";
        OAuthProvider provider = OAuthProvider.NAVER;
        String memberKey = "naver-member";

        OAuthSignUpCommand command = new OAuthSignUpCommand(
                providerMemberId,
                smsToken,
                "네이버사용자",
                LocalDate.of(1997, 9, 10),
                Gender.FEMALE,
                MemberRole.GUARDIAN
        );

        PhoneNumber phone = SmsFixture.createPhoneNumber();
        Member member = MemberFixture.createMember();
        Jwt jwt = new Jwt("naver-access", "naver-refresh");

        given(phoneVerificationReader.findPhoneByToken(smsToken)).willReturn(phone);
        given(oAuthManager.register(any(NewOauthMember.class))).willReturn(memberKey);
        given(memberReader.findByMemberKey(memberKey)).willReturn(member);
        given(tokenIssuer.issue(member)).willReturn(jwt);

        // when
        Jwt result = oAuthService.signUp(provider, command);

        // then
        assertThat(result.refreshToken()).isEqualTo("naver-refresh");
    }

    @Test
    @DisplayName("OAuth 회원가입 - 트랜잭션 내 모든 작업 순서 검증")
    void signUp_transactional_order() {
        // given
        String providerMemberId = "user-tx-123";
        String smsToken = "tx-token";
        OAuthProvider provider = OAuthProvider.KAKAO;
        String memberKey = "tx-member";

        OAuthSignUpCommand command = new OAuthSignUpCommand(
                providerMemberId,
                smsToken,
                "트랜잭션사용자",
                LocalDate.of(1995, 3, 25),
                Gender.MALE,
                MemberRole.GUARDIAN
        );

        PhoneNumber phone = SmsFixture.createPhoneNumber();
        Member member = MemberFixture.createMember();
        Jwt jwt = AuthFixture.createJwt();

        given(phoneVerificationReader.findPhoneByToken(smsToken)).willReturn(phone);
        given(oAuthManager.register(any(NewOauthMember.class))).willReturn(memberKey);
        given(memberReader.findByMemberKey(memberKey)).willReturn(member);
        given(tokenIssuer.issue(member)).willReturn(jwt);

        // when
        Jwt result = oAuthService.signUp(provider, command);

        // then
        assertThat(result).isNotNull();
        then(phoneVerificationReader).should(times(1)).findPhoneByToken(smsToken);
        then(oAuthManager).should(times(1)).register(any(NewOauthMember.class));
        then(memberReader).should(times(1)).findByMemberKey(memberKey);
        then(tokenIssuer).should(times(1)).issue(member);
    }
}
