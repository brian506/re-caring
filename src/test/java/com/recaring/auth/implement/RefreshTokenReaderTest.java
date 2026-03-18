package com.recaring.auth.implement;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenReader 단위 테스트")
class RefreshTokenReaderTest {

    @InjectMocks
    private RefreshTokenReader refreshTokenReader;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("리프레시 토큰으로 memberKey 조회 성공")
    void findMemberKey_success() {
        // given
        String refreshToken = "valid-refresh-token";
        String memberKey = "member-key-123";
        String redisKey = "refresh:token:" + refreshToken;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(redisKey)).willReturn(memberKey);

        // when
        String result = refreshTokenReader.findMemberKey(refreshToken);

        // then
        assertThat(result).isEqualTo(memberKey);
    }

    @Test
    @DisplayName("리프레시 토큰으로 memberKey 조회 실패 - 만료된 토큰")
    void findMemberKey_fail_expired_token() {
        // given
        String expiredToken = "expired-refresh-token";
        String redisKey = "refresh:token:" + expiredToken;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(redisKey)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> refreshTokenReader.findMemberKey(expiredToken))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.EXPIRED_JWT);
    }

    @Test
    @DisplayName("리프레시 토큰으로 memberKey 조회 - Redis 키 프리픽스 검증")
    void findMemberKey_redis_key_prefix() {
        // given
        String refreshToken = "token-abc-123";
        String memberKey = "member-xyz-789";
        String expectedRedisKey = "refresh:token:token-abc-123";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedRedisKey)).willReturn(memberKey);

        // when
        String result = refreshTokenReader.findMemberKey(refreshToken);

        // then
        assertThat(result).isEqualTo(memberKey);
    }
}
