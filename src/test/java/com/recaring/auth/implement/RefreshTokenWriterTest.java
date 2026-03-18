package com.recaring.auth.implement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenWriter 단위 테스트")
class RefreshTokenWriterTest {

    @InjectMocks
    private RefreshTokenWriter refreshTokenWriter;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("리프레시 토큰 저장 성공 - Redis에 memberKey와 함께 저장")
    void save_success() {
        // given
        String refreshToken = "refresh-token-123";
        String memberKey = "member-key-456";
        long refreshExpiration = 1209600000; // 14 days in milliseconds

        ReflectionTestUtils.setField(refreshTokenWriter, "refreshExpiration", refreshExpiration);

        org.mockito.Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        refreshTokenWriter.save(refreshToken, memberKey);

        // then
        then(redisTemplate).should(times(1)).opsForValue();
        then(valueOperations).should(times(1))
            .set("refresh:token:" + refreshToken, memberKey, refreshExpiration, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("리프레시 토큰 삭제 성공 - Redis에서 토큰 제거")
    void delete_success() {
        // given
        String refreshToken = "refresh-token-to-delete";
        String redisKey = "refresh:token:" + refreshToken;

        org.mockito.Mockito.when(redisTemplate.delete(redisKey)).thenReturn(1L);

        // when
        refreshTokenWriter.delete(refreshToken);

        // then
        then(redisTemplate).should(times(1)).delete(redisKey);
    }

    @Test
    @DisplayName("리프레시 토큰 저장 - 올바른 Redis 키 프리픽스 사용")
    void save_redis_key_prefix() {
        // given
        String refreshToken = "token-prefix-test";
        String memberKey = "member-123";
        long expiration = 1209600000;
        String expectedKey = "refresh:token:token-prefix-test";

        ReflectionTestUtils.setField(refreshTokenWriter, "refreshExpiration", expiration);
        org.mockito.Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        refreshTokenWriter.save(refreshToken, memberKey);

        // then
        then(valueOperations).should(times(1))
            .set(expectedKey, memberKey, expiration, TimeUnit.MILLISECONDS);
    }
}
