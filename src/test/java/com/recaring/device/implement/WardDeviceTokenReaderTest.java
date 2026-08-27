package com.recaring.device.implement;

import com.recaring.device.dataaccess.entity.WardDeviceToken;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.device.fixture.DeviceFixture;
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

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("WardDeviceTokenReader 단위 테스트")
class WardDeviceTokenReaderTest {

    private static final String TOKEN = "test-token";
    private static final String CACHE_KEY = "deviceToken:" + TOKEN;

    @InjectMocks
    private WardDeviceTokenReader wardDeviceTokenReader;

    @Mock
    private WardDeviceTokenRepository wardDeviceTokenRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("캐시에 wardKey가 있으면 DB를 조회하지 않는다")
    void getByToken_returns_cached_wardKey_without_db_query() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn(DeviceFixture.WARD_KEY);

        // when
        String result = wardDeviceTokenReader.getByToken(TOKEN);

        // then
        assertThat(result).isEqualTo(DeviceFixture.WARD_KEY);
        then(wardDeviceTokenRepository).should(never()).findByToken(any());
    }

    @Test
    @DisplayName("캐시가 비어 있으면 DB에서 찾은 wardKey를 30일 TTL로 캐시에 채운다")
    void getByToken_queries_db_on_cache_miss_and_stores_in_redis() {
        // given
        WardDeviceToken entity = DeviceFixture.createDeviceToken(DeviceFixture.WARD_KEY);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn(null);
        given(wardDeviceTokenRepository.findByToken(TOKEN)).willReturn(Optional.of(entity));

        // when
        String result = wardDeviceTokenReader.getByToken(TOKEN);

        // then
        assertThat(result).isEqualTo(DeviceFixture.WARD_KEY);
        then(valueOperations).should().set(CACHE_KEY, DeviceFixture.WARD_KEY, Duration.ofDays(30));
    }

    @Test
    @DisplayName("캐시에도 DB에도 없는 토큰이면 INVALID_DEVICE_TOKEN 예외가 발생한다")
    void getByToken_throws_when_token_not_found_in_db() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn(null);
        given(wardDeviceTokenRepository.findByToken(TOKEN)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> wardDeviceTokenReader.getByToken(TOKEN))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_DEVICE_TOKEN);
    }

    @Test
    @DisplayName("무효화는 조회와 동일한 캐시 키를 지운다")
    void evict_deletes_the_same_cache_key_used_by_lookup() {
        // when
        wardDeviceTokenReader.evict(TOKEN);

        // then
        then(stringRedisTemplate).should().delete(CACHE_KEY);
    }
}
