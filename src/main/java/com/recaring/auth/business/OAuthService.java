package com.recaring.auth.business;

import com.recaring.auth.business.command.OAuthSignUpCommand;
import com.recaring.auth.controller.response.OAuthSignInResponse;
import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.implement.oauth.OAuthAuthenticator;
import com.recaring.auth.implement.oauth.OAuthManager;
import com.recaring.auth.implement.oauth.OAuthReader;
import com.recaring.auth.implement.TokenIssuer;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;
import com.recaring.domain.member.dataaccess.entity.Member;
import com.recaring.domain.member.implement.MemberReader;
import com.recaring.security.vo.Jwt;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final List<OAuthAuthenticator> authenticators;
    private final OAuthReader oAuthReader;
    private final OAuthManager oAuthManager;
    private final MemberReader memberReader;
    private final TokenIssuer tokenIssuer;
    private final PhoneVerificationReader phoneVerificationReader;

    public OAuthSignInResponse signIn(String accessToken, OAuthProvider provider) {
        OAuthUser oAuthUser = authenticate(accessToken, provider);
        Optional<OAuth> oAuth = oAuthReader.findOAuthUser(provider, oAuthUser.providerUserId());

        if (oAuth.isEmpty()) {
            return OAuthSignInResponse.needSignUp(oAuthUser.providerUserId());
        }

        Member member = memberReader.findByMemberKey(oAuth.get().getMemberKey());
        return OAuthSignInResponse.success(tokenIssuer.issue(member));
    }

    @Transactional
    public Jwt signUp(OAuthProvider provider, OAuthSignUpCommand command) {
        PhoneNumber phone = phoneVerificationReader.findPhoneByToken(command.smsToken());
        String memberKey = oAuthManager.register(command.toNewOauthMember(phone, provider));
        Member member = memberReader.findByMemberKey(memberKey);
        return tokenIssuer.issue(member);
    }

    private OAuthUser authenticate(String accessToken, OAuthProvider provider) {
        return authenticators.stream()
                .filter(a -> a.supports(provider))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorType.INVALID_OAUTH_USER))
                .authentication(accessToken);
    }
}
