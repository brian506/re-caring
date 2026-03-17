package com.recaring.auth.implement;

import com.recaring.support.AbstractIntegrationTest;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RefreshTokenReader 통합 테스트")
class RefreshTokenReaderTest extends AbstractIntegrationTest {

    private static final String KEY_PREFIX = "refresh:token:";

    @Autowired
    private RefreshTokenReader refreshTokenReader;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("저장된 리프레시 토큰으로 memberKey를 조회할 수 있다")
    void findMemberKey_success() {
        String refreshToken = UUID.randomUUID().toString();
        String memberKey = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + refreshToken, memberKey, 1, TimeUnit.HOURS);

        String result = refreshTokenReader.findMemberKey(refreshToken);

        assertThat(result).isEqualTo(memberKey);
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 조회하면 EXPIRED_JWT 예외가 발생한다")
    void findMemberKey_fail_when_token_not_found() {
        String nonExistingToken = UUID.randomUUID().toString();

        assertThatThrownBy(() -> refreshTokenReader.findMemberKey(nonExistingToken))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.EXPIRED_JWT);
    }
}
