package com.recaring.device.implement;

import com.recaring.device.dataaccess.entity.WardDeviceToken;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("WardDeviceTokenReader 단위 테스트")
class WardDeviceTokenReaderTest {

    @InjectMocks
    private WardDeviceTokenReader wardDeviceTokenReader;

    @Mock
    private WardDeviceTokenRepository wardDeviceTokenRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("Redis에 캐시가 있으면 DB를 조회하지 않고 wardKey를 반환한다")
    void getByToken_returns_cached_wardKey_without_db_query() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("deviceToken:test-token")).willReturn("ward-key-001");

        String result = wardDeviceTokenReader.getByToken("test-token");

        assertThat(result).isEqualTo("ward-key-001");
        then(wardDeviceTokenRepository).should(never()).findByToken(any());
    }

    @Test
    @DisplayName("Redis 캐시 미스 시 DB를 조회하고 결과를 Redis에 저장한다")
    void getByToken_queries_db_on_cache_miss_and_stores_in_redis() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        WardDeviceToken entity = WardDeviceToken.builder().wardKey("ward-key-001").build();
        given(valueOps.get("deviceToken:test-token")).willReturn(null);
        given(wardDeviceTokenRepository.findByToken("test-token")).willReturn(Optional.of(entity));

        String result = wardDeviceTokenReader.getByToken("test-token");

        assertThat(result).isEqualTo("ward-key-001");
        then(valueOps).should().set(eq("deviceToken:test-token"), eq("ward-key-001"), any());
    }

    @Test
    @DisplayName("DB에도 토큰이 없으면 INVALID_DEVICE_TOKEN 예외가 발생한다")
    void getByToken_throws_when_token_not_found_in_db() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("deviceToken:unknown-token")).willReturn(null);
        given(wardDeviceTokenRepository.findByToken("unknown-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> wardDeviceTokenReader.getByToken("unknown-token"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_DEVICE_TOKEN);
    }

    @Test
    @DisplayName("evict() 호출 시 Redis에서 해당 토큰 키를 삭제한다")
    void evict_deletes_token_from_redis() {
        wardDeviceTokenReader.evict("old-token");

        then(stringRedisTemplate).should().delete("deviceToken:old-token");
    }
}
