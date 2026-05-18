package com.recaring.auth.implement;

import com.recaring.auth.dataaccess.entity.RefreshToken;
import com.recaring.auth.dataaccess.repository.RefreshTokenRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenReader {

    private final RefreshTokenRepository refreshTokenRepository;

    public String findMemberKey(String refreshToken) {
        RefreshToken entity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AppException(ErrorType.EXPIRED_JWT));

        if (entity.isExpired()) {
            refreshTokenRepository.deleteByToken(refreshToken);
            throw new AppException(ErrorType.EXPIRED_JWT);
        }

        return entity.getMemberKey();
    }
}
