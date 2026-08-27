package com.recaring.auth.implement.oauth;

import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;
import com.recaring.auth.vo.naver.NaverResponse;
import com.recaring.auth.vo.naver.NaverUser;
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
@DisplayName("NaverAuthenticator 단위 테스트")
class NaverAuthenticatorTest {

    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";
    private static final String ACCESS_TOKEN = "naver-access-token";
    private static final String AUTHORIZATION_VALUE = "Bearer " + ACCESS_TOKEN;

    private static final String SUCCESS_RESULT_CODE = "00";
    private static final String NAVER_ID = "naver-user-9876";
    private static final String NAVER_EMAIL = "naver-user@example.com";
    private static final String NAVER_NAME = "네이버이름";

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
    private NaverAuthenticator naverAuthenticator;

    @SuppressWarnings("unchecked")
    private void givenNaverResponds(NaverUser body) {
        given(restClient.get()).willReturn(uriSpec);
        given(uriSpec.uri(USER_INFO_URL)).willReturn(headersSpec);
        given(headersSpec.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_VALUE)).willReturn(headersSpec);
        given(headersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(NaverUser.class)).willReturn(body);
    }

    @Test
    @DisplayName("네이버가 내려준 response 안의 id·이메일·이름이 OAuthUser로 매핑된다")
    void authenticate_maps_naver_response_to_oauth_user() {
        // given
        givenNaverResponds(new NaverUser(
                SUCCESS_RESULT_CODE,
                "success",
                new NaverResponse(NAVER_ID, NAVER_EMAIL, NAVER_NAME)
        ));

        // when
        OAuthUser result = naverAuthenticator.authenticate(ACCESS_TOKEN);

        // then
        assertThat(result.providerMemberId()).isEqualTo(NAVER_ID);
        assertThat(result.provider()).isEqualTo(OAuthProvider.NAVER);
        assertThat(result.email()).isEqualTo(NAVER_EMAIL);
        assertThat(result.name()).isEqualTo(NAVER_NAME);
    }

    @Test
    @DisplayName("네이버 응답 본문이 비어 있으면 INVALID_OAUTH_USER 예외가 발생한다")
    void authenticate_throws_when_response_body_is_null() {
        // given
        givenNaverResponds(null);

        // when & then
        assertThatThrownBy(() -> naverAuthenticator.authenticate(ACCESS_TOKEN))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_OAUTH_USER);
    }

    @Test
    @DisplayName("NAVER provider만 지원한다")
    void supports_only_naver() {
        assertThat(naverAuthenticator.supports(OAuthProvider.NAVER)).isTrue();
        assertThat(naverAuthenticator.supports(OAuthProvider.KAKAO)).isFalse();
    }
}
