package com.recaring.auth.implement;

import com.recaring.member.vo.MemberInfo;
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

    public Jwt issue(MemberInfo member) {
        Jwt jwt = jwtGenerator.generateJwt(new TokenPayload(member.memberKey(), member.role(), new Date()));
        refreshTokenWriter.save(jwt.refreshToken(), member.memberKey());
        return jwt;
    }
}
