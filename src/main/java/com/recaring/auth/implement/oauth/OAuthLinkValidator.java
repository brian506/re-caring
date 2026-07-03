package com.recaring.auth.implement.oauth;

import com.recaring.auth.vo.OAuthProvider;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthLinkValidator {

    private final OAuthReader oAuthReader;

    public void validateLinkable(String memberKey, OAuthProvider provider) {
        if (oAuthReader.isLinked(memberKey, provider)) {
            throw new AppException(ErrorType.OAUTH_ALREADY_LINKED);
        }
    }
}
