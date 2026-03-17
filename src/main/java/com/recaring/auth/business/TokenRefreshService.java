package com.recaring.auth.business;

import com.recaring.auth.implement.RefreshTokenReader;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.domain.member.Member;
import com.recaring.domain.member.implement.MemberReader;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.security.jwt.JwtValidator;
import com.recaring.security.vo.Jwt;
import com.recaring.security.vo.TokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenRefreshService {
    private final JwtGenerator jwtGenerator;
    private final JwtValidator jwtValidator;
    private final RefreshTokenReader refreshTokenReader;
    private final RefreshTokenWriter refreshTokenWriter;
    private final MemberReader memberReader;

    public Jwt refresh(String refreshToken) {
        jwtValidator.validate(refreshToken);
        String memberKey = refreshTokenReader.findMemberKey(refreshToken);
        Member member = memberReader.findByMemberKey(memberKey);

        refreshTokenWriter.delete(refreshToken);

        Jwt jwt = jwtGenerator.generateJwt(new TokenPayload(member.getMemberKey(), member.getRole(), new Date()));
        refreshTokenWriter.save(jwt.refreshToken(), member.getMemberKey());

        return jwt;
    }

}
