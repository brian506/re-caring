package com.recaring.auth.business;

import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.implement.TokenIssuer;
import com.recaring.auth.implement.oauth.OAuthAuthenticator;
import com.recaring.auth.implement.oauth.OAuthManager;
import com.recaring.auth.implement.oauth.OAuthReader;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.security.vo.Jwt;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthService 단위 테스트")
class OAuthServiceTest {

    private static final String KAKAO_ACCESS_TOKEN = "kakao-access-token";
    private static final String NAVER_ACCESS_TOKEN = "naver-access-token";
    private static final String KAKAO_MEMBER_ID = "kakao-user-123";
    private static final String NAVER_MEMBER_ID = "naver-user-456";
    private static final String LINKED_MEMBER_KEY = "linked-member-key";
    private static final String LOGIN_MEMBER_KEY = "login-member-key";

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
                tokenIssuer
        );
    }

    @Test
    @DisplayName("연동된 소셜 계정으로 로그인하면 연동된 회원의 JWT가 발급된다")
    void signIn_issues_jwt_for_linked_member() {
        // given
        OAuthUser oAuthUser = new OAuthUser(KAKAO_MEMBER_ID, OAuthProvider.KAKAO, "user@example.com", "카카오사용자");
        OAuth oAuth = OAuth.builder()
                .memberKey(LINKED_MEMBER_KEY)
                .provider(OAuthProvider.KAKAO)
                .providerMemberId(KAKAO_MEMBER_ID)
                .build();
        Member linkedMember = MemberFixture.createMemberWithKey(LINKED_MEMBER_KEY, MemberFixture.PHONE);

        given(kakaoAuthenticator.supports(OAuthProvider.KAKAO)).willReturn(true);
        given(kakaoAuthenticator.authenticate(KAKAO_ACCESS_TOKEN)).willReturn(oAuthUser);
        given(oAuthReader.find(OAuthProvider.KAKAO, KAKAO_MEMBER_ID)).willReturn(Optional.of(oAuth));
        given(memberReader.findByMemberKey(LINKED_MEMBER_KEY)).willReturn(linkedMember);
        given(tokenIssuer.issue(linkedMember)).willReturn(AuthFixture.createJwt());

        // when
        Jwt result = oAuthService.signIn(KAKAO_ACCESS_TOKEN, OAuthProvider.KAKAO);

        // then
        assertThat(result.accessToken()).isEqualTo(AuthFixture.ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(AuthFixture.REFRESH_TOKEN);
        then(naverAuthenticator).should(never()).authenticate(anyString());
    }

    @Test
    @DisplayName("연동되지 않은 소셜 계정으로 로그인하면 OAUTH_NOT_LINKED 예외가 발생한다")
    void signIn_fails_when_not_linked() {
        // given
        OAuthUser oAuthUser = new OAuthUser(NAVER_MEMBER_ID, OAuthProvider.NAVER, "newuser@example.com", "네이버사용자");

        given(naverAuthenticator.supports(OAuthProvider.NAVER)).willReturn(true);
        given(naverAuthenticator.authenticate(NAVER_ACCESS_TOKEN)).willReturn(oAuthUser);
        given(oAuthReader.find(OAuthProvider.NAVER, NAVER_MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> oAuthService.signIn(NAVER_ACCESS_TOKEN, OAuthProvider.NAVER))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.OAUTH_NOT_LINKED);

        then(memberReader).should(never()).findByMemberKey(anyString());
        then(tokenIssuer).should(never()).issue(any());
    }

    @Test
    @DisplayName("지원하는 authenticator가 없으면 로그인 시 INVALID_OAUTH_USER 예외가 발생한다")
    void signIn_fails_unsupported_provider() {
        // given
        given(kakaoAuthenticator.supports(OAuthProvider.KAKAO)).willReturn(false);
        given(naverAuthenticator.supports(OAuthProvider.KAKAO)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> oAuthService.signIn(KAKAO_ACCESS_TOKEN, OAuthProvider.KAKAO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_OAUTH_USER);

        then(kakaoAuthenticator).should(never()).authenticate(anyString());
        then(oAuthReader).should(never()).find(any(), anyString());
    }

    @Test
    @DisplayName("소셜 연동 시 로그인한 회원과 인증된 소셜 식별자가 함께 전달된다")
    void link_success() {
        // given
        OAuthUser oAuthUser = new OAuthUser(KAKAO_MEMBER_ID, OAuthProvider.KAKAO, "user@example.com", "카카오사용자");

        given(kakaoAuthenticator.supports(OAuthProvider.KAKAO)).willReturn(true);
        given(kakaoAuthenticator.authenticate(KAKAO_ACCESS_TOKEN)).willReturn(oAuthUser);

        // when
        oAuthService.link(LOGIN_MEMBER_KEY, OAuthProvider.KAKAO, KAKAO_ACCESS_TOKEN);

        // then
        then(oAuthManager).should(times(1)).link(LOGIN_MEMBER_KEY, OAuthProvider.KAKAO, KAKAO_MEMBER_ID);
    }

    @Test
    @DisplayName("지원하는 authenticator가 없으면 연동을 위임하지 않는다")
    void link_fails_unsupported_provider() {
        // given
        given(kakaoAuthenticator.supports(OAuthProvider.NAVER)).willReturn(false);
        given(naverAuthenticator.supports(OAuthProvider.NAVER)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> oAuthService.link(LOGIN_MEMBER_KEY, OAuthProvider.NAVER, NAVER_ACCESS_TOKEN))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_OAUTH_USER);

        then(oAuthManager).should(never()).link(anyString(), any(), anyString());
    }
}
