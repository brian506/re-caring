package com.recaring.auth.implement.oauth;

import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;
import com.recaring.auth.vo.kakao.KakaoAccount;
import com.recaring.auth.vo.kakao.KakaoProfile;
import com.recaring.auth.vo.kakao.KakaoUser;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoAuthenticator 단위 테스트")
class KakaoAuthenticatorTest {

    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String ACCESS_TOKEN = "kakao-access-token";
    private static final String AUTHORIZATION_VALUE = "Bearer " + ACCESS_TOKEN;

    private static final Long KAKAO_ID = 4321L;
    private static final String KAKAO_EMAIL = "kakao-user@example.com";
    private static final String KAKAO_NICKNAME = "카카오닉네임";

    @Mock
    private RestClient restClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec uriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersSpec headersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private KakaoAuthenticator kakaoAuthenticator;

    @SuppressWarnings("unchecked")
    private void givenKakaoResponds(KakaoUser body) {
        given(restClient.get()).willReturn(uriSpec);
        given(uriSpec.uri(USER_INFO_URL)).willReturn(headersSpec);
        given(headersSpec.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_VALUE)).willReturn(headersSpec);
        given(headersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(KakaoUser.class)).willReturn(body);
    }

    @Test
    @DisplayName("카카오가 내려준 id·이메일·닉네임이 OAuthUser로 매핑된다")
    void authenticate_maps_kakao_response_to_oauth_user() {
        // given
        givenKakaoResponds(new KakaoUser(
                KAKAO_ID,
                new KakaoAccount(KAKAO_EMAIL, new KakaoProfile(KAKAO_NICKNAME))
        ));

        // when
        OAuthUser result = kakaoAuthenticator.authenticate(ACCESS_TOKEN);

        // then
        assertThat(result.providerMemberId()).isEqualTo("4321");
        assertThat(result.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(result.email()).isEqualTo(KAKAO_EMAIL);
        assertThat(result.name()).isEqualTo(KAKAO_NICKNAME);
    }

    @Test
    @DisplayName("카카오 계정 동의 항목이 비어 있으면 이메일과 이름 없이 매핑된다")
    void authenticate_maps_without_account_information() {
        givenKakaoResponds(new KakaoUser(KAKAO_ID, null));

        // when
        OAuthUser result = kakaoAuthenticator.authenticate(ACCESS_TOKEN);

        // then
        assertThat(result.providerMemberId()).isEqualTo("4321");
        assertThat(result.email()).isNull();
        assertThat(result.name()).isNull();
    }

    @Test
    @DisplayName("카카오 프로필만 비어 있으면 이메일은 유지하고 이름만 비운다")
    void authenticate_maps_without_profile() {
        // given
        givenKakaoResponds(new KakaoUser(KAKAO_ID, new KakaoAccount(KAKAO_EMAIL, null)));

        // when
        OAuthUser result = kakaoAuthenticator.authenticate(ACCESS_TOKEN);

        // then
        assertThat(result.email()).isEqualTo(KAKAO_EMAIL);
        assertThat(result.name()).isNull();
    }

    @Test
    @DisplayName("카카오 응답 본문이 비어 있으면 INVALID_OAUTH_USER 예외가 발생한다")
    void authenticate_throws_when_response_body_is_null() {
        // given
        givenKakaoResponds(null);

        // when & then
        assertThatThrownBy(() -> kakaoAuthenticator.authenticate(ACCESS_TOKEN))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_OAUTH_USER);
    }

    @Test
    @DisplayName("KAKAO provider만 지원한다")
    void supports_only_kakao() {
        assertThat(kakaoAuthenticator.supports(OAuthProvider.KAKAO)).isTrue();
        assertThat(kakaoAuthenticator.supports(OAuthProvider.NAVER)).isFalse();
    }
}
