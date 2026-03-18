package com.recaring.auth.implement.oauth;

import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;
import com.recaring.auth.vo.naver.NaverUser;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class NaverAuthenticator implements OAuthAuthenticator {

    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";
    private static final String BEARER = "Bearer ";

    private final RestClient restClient;

    @Override
    public OAuthUser authentication(String accessToken) {
        NaverUser naverUser = restClient.get()
                .uri(NAVER_USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, BEARER + accessToken)
                .retrieve()
                .body(NaverUser.class);

        if (naverUser == null) {
            throw new AppException(ErrorType.INVALID_OAUTH_USER);
        }

        return naverUser.toOAuthUser();
    }

    @Override
    public boolean supports(OAuthProvider provider) {
        return OAuthProvider.NAVER.equals(provider);
    }
}
