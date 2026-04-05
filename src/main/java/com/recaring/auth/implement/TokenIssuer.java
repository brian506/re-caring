package com.recaring.auth.implement;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.security.vo.Jwt;
import com.recaring.security.vo.TokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtGenerator jwtGenerator;
    private final RefreshTokenWriter refreshTokenWriter;

    public Jwt issue(Member member) {
        Jwt jwt = jwtGenerator.generateJwt(new TokenPayload(member.getMemberKey(), member.getRole(), new Date()));
        refreshTokenWriter.save(jwt.refreshToken(), member.getMemberKey());
        return jwt;
    }
}
