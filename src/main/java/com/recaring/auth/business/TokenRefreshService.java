package com.recaring.auth.business;

import com.recaring.auth.implement.RefreshTokenReader;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.implement.TokenIssuer;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.security.jwt.JwtValidator;
import com.recaring.security.vo.Jwt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final JwtValidator jwtValidator;
    private final RefreshTokenReader refreshTokenReader;
    private final RefreshTokenWriter refreshTokenWriter;
    private final MemberReader memberReader;
    private final TokenIssuer tokenIssuer;

    public Jwt refresh(String refreshToken) {
        jwtValidator.validate(refreshToken);
        String memberKey = refreshTokenReader.findMemberKey(refreshToken);
        Member member = memberReader.findByMemberKey(memberKey);
        refreshTokenWriter.delete(refreshToken);
        return tokenIssuer.issue(member);
    }
}
