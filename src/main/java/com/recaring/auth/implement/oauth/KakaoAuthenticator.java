package com.recaring.auth.implement.oauth;

import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;
import com.recaring.auth.vo.kakao.KakaoUser;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import org.springframework.http.HttpHeaders;

@Component
@RequiredArgsConstructor
public class KakaoAuthenticator implements OAuthAuthenticator {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String BEARER = "Bearer ";

    private final RestClient restClient;

    @Override
    public OAuthUser authentication(String accessToken) {
        KakaoUser kakaoUser = restClient.get()
                .uri(KAKAO_USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, BEARER + accessToken)
                .retrieve()
                .body(KakaoUser.class);

        if (kakaoUser == null) {
            throw new AppException(ErrorType.INVALID_OAUTH_USER);
        }

        return kakaoUser.toOAuthUser();
    }

    @Override
    public boolean supports(OAuthProvider provider) {
        return OAuthProvider.KAKAO.equals(provider);
    }
}
