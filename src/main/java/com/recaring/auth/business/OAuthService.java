package com.recaring.auth.business;

import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.implement.oauth.OAuthAuthenticator;
import com.recaring.auth.implement.oauth.OAuthManager;
import com.recaring.auth.implement.oauth.OAuthReader;
import com.recaring.auth.implement.TokenIssuer;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.security.vo.Jwt;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final List<OAuthAuthenticator> authenticators;
    private final OAuthReader oAuthReader;
    private final OAuthManager oAuthManager;
    private final MemberReader memberReader;
    private final TokenIssuer tokenIssuer;

    public Jwt signIn(String accessToken, OAuthProvider provider) {
        OAuthUser oAuthUser = authenticate(accessToken, provider);
        OAuth oAuth = oAuthReader.findOAuthUser(provider, oAuthUser.providerMemberId())
                .orElseThrow(() -> new AppException(ErrorType.OAUTH_NOT_LINKED));

        Member member = memberReader.findByMemberKey(oAuth.getMemberKey());
        return tokenIssuer.issue(member);
    }

    public void link(String memberKey, OAuthProvider provider, String accessToken) {
        OAuthUser oAuthUser = authenticate(accessToken, provider);
        oAuthManager.link(memberKey, provider, oAuthUser.providerMemberId());
    }

    private OAuthUser authenticate(String accessToken, OAuthProvider provider) {
        return authenticators.stream()
                .filter(a -> a.supports(provider))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorType.INVALID_OAUTH_USER))
                .authentication(accessToken);
    }
}
