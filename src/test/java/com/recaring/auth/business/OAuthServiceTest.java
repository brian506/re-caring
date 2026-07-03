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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
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
    @DisplayName("OAuth 로그인 성공 - 연동된 계정이면 JWT를 발급한다")
    void signIn_success_linked_account() {
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
        Jwt result = oAuthService.signIn(accessToken, provider);

        // then
        assertThat(result.accessToken()).isEqualTo(jwt.accessToken());
        assertThat(result.refreshToken()).isEqualTo(jwt.refreshToken());
    }

    @Test
    @DisplayName("OAuth 로그인 실패 - 연동되지 않은 계정이면 OAUTH_NOT_LINKED 예외가 발생한다")
    void signIn_fails_when_not_linked() {
        // given
        String accessToken = "naver-access-token";
        OAuthProvider provider = OAuthProvider.NAVER;
        String providerMemberId = "naver-user-456";

        OAuthUser oAuthUser = new OAuthUser(providerMemberId, provider, "newuser@example.com", "네이버사용자");

        given(naverAuthenticator.supports(provider)).willReturn(true);
        given(naverAuthenticator.authentication(accessToken)).willReturn(oAuthUser);
        given(oAuthReader.findOAuthUser(provider, providerMemberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> oAuthService.signIn(accessToken, provider))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.OAUTH_NOT_LINKED);

        then(tokenIssuer).should(never()).issue(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("OAuth 로그인 실패 - 지원하지 않는 provider")
    void signIn_fails_unsupported_provider() {
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
    @DisplayName("OAuth 연동 성공 - provider 인증 후 memberKey에 연동을 위임한다")
    void link_success() {
        // given
        String memberKey = "login-member-key";
        String accessToken = "kakao-access-token";
        OAuthProvider provider = OAuthProvider.KAKAO;
        String providerMemberId = "kakao-user-999";

        OAuthUser oAuthUser = new OAuthUser(providerMemberId, provider, "user@example.com", "카카오사용자");

        given(kakaoAuthenticator.supports(provider)).willReturn(true);
        given(kakaoAuthenticator.authentication(accessToken)).willReturn(oAuthUser);

        // when
        oAuthService.link(memberKey, provider, accessToken);

        // then
        then(oAuthManager).should(times(1)).link(memberKey, provider, providerMemberId);
    }

    @Test
    @DisplayName("OAuth 연동 실패 - 지원하지 않는 provider면 연동을 위임하지 않는다")
    void link_fails_unsupported_provider() {
        // given
        String memberKey = "login-member-key";
        String accessToken = "some-token";
        OAuthProvider provider = OAuthProvider.NAVER;

        given(kakaoAuthenticator.supports(provider)).willReturn(false);
        given(naverAuthenticator.supports(provider)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> oAuthService.link(memberKey, provider, accessToken))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_OAUTH_USER);

        then(oAuthManager).should(never()).link(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
