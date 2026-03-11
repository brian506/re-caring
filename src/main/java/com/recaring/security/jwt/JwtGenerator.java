package com.recaring.security.jwt;

import com.recaring.security.vo.TokenPayload;
import com.recaring.security.vo.Jwt;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtGenerator {

    @Value("${spring.application.name}")
    private String issuer;

    @Value("${jwt.access.expiration}")
    private long accessKeyExpiration;

    @Value("${jwt.refresh.expiration}")
    private long refreshKeyExpiration;

    private final SecretKey secretKey;

    public Jwt generateJwt(TokenPayload payload) {
        String memberKey = payload.memberKey();
        if(memberKey == null || memberKey.isBlank()) {
            throw new AppException(ErrorType.INVALID_MEMBER_KEY);
        }
        return new Jwt(createAccessToken(payload), createRefreshToken(payload));
    }


    private String createAccessToken(TokenPayload payload) {
        return Jwts.builder()
                .subject(payload.memberKey())
                .claim("role", payload.role())
                .issuer(issuer)
                .issuedAt(payload.date())
                .expiration(new Date(payload.date().getTime() + accessKeyExpiration))
                .signWith(secretKey)
                .compact();
    }

    private String createRefreshToken(TokenPayload payload) {
        return Jwts.builder()
                .subject(payload.memberKey())
                .issuer(issuer)
                .issuedAt(payload.date())
                .expiration(new Date(payload.date().getTime() + refreshKeyExpiration))
                .signWith(secretKey)
                .compact();
    }
}
