package com.recaring.auth.implement;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenReader {

    private static final String KEY_PREFIX = "refresh:token:";

    private final StringRedisTemplate redisTemplate;

    public String findMemberKey(String refreshToken) {
        String memberKey = redisTemplate.opsForValue().get(KEY_PREFIX + refreshToken);
        if (memberKey == null) {
            throw new AppException(ErrorType.EXPIRED_JWT);
        }
        return memberKey;
    }
}
