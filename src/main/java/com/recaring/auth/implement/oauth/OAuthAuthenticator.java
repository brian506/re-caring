package com.recaring.auth.implement.oauth;

import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;

public interface OAuthAuthenticator {
    OAuthUser authentication(String accessToken);
    boolean supports(OAuthProvider provider);
}
