package com.recaring.auth.implement.oauth;

import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;

public interface OAuthAuthenticator {
    OAuthUser authenticate(String accessToken);
    boolean supports(OAuthProvider provider);
}
