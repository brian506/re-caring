package com.recaring.auth.implement;

import com.recaring.auth.dataaccess.entity.RefreshToken;
import com.recaring.auth.dataaccess.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenWriter {

    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration; // ms 단위

    private final RefreshTokenRepository refreshTokenRepository;

    public void save(String refreshToken, String memberKey) {
        RefreshToken entity = RefreshToken.of(memberKey, refreshToken, refreshExpiration);
        refreshTokenRepository.save(entity);
    }

    public void delete(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }
}
