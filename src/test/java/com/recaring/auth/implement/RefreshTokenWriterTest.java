package com.recaring.auth.implement;

import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshTokenWriter 통합 테스트")
class RefreshTokenWriterTest extends AbstractIntegrationTest {

    private static final String KEY_PREFIX = "refresh:token:";

    @Autowired
    private RefreshTokenWriter refreshTokenWriter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("save() 호출 시 리프레시 토큰이 Redis에 저장된다")
    void save_stores_token_in_redis() {
        String refreshToken = UUID.randomUUID().toString();
        String memberKey = UUID.randomUUID().toString();

        refreshTokenWriter.save(refreshToken, memberKey);

        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + refreshToken);
        assertThat(stored).isEqualTo(memberKey);
    }

    @Test
    @DisplayName("save() 후 delete() 호출 시 토큰이 Redis에서 삭제된다")
    void delete_removes_token_from_redis() {
        String refreshToken = UUID.randomUUID().toString();
        String memberKey = UUID.randomUUID().toString();

        refreshTokenWriter.save(refreshToken, memberKey);
        refreshTokenWriter.delete(refreshToken);

        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + refreshToken);
        assertThat(stored).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 토큰을 delete()해도 예외가 발생하지 않는다")
    void delete_non_existing_token_does_not_throw() {
        String nonExistingToken = UUID.randomUUID().toString();

        refreshTokenWriter.delete(nonExistingToken);

        // 예외 없이 통과
    }
}
