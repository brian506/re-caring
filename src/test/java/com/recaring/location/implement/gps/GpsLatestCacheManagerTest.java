package com.recaring.location.implement.gps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.Gps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import org.springframework.data.redis.RedisConnectionFailureException;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThatCode;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GpsLatestCacheManager 단위 테스트")
class GpsLatestCacheManagerTest {

    @InjectMocks
    private GpsLatestCacheManager manager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("Redis에 값이 있으면 역직렬화하여 Gps를 반환한다")
    void find_returns_gps_when_cache_exists() throws Exception {
        String json = "{\"latitude\":37.5665,\"longitude\":126.9780,\"recordedAt\":\"2024-01-01T00:00:00\"}";
        Gps expected = LocationFixture.createGps();
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("gps:latest:" + LocationFixture.WARD_KEY)).willReturn(json);
        given(objectMapper.readValue(json, Gps.class)).willReturn(expected);

        Optional<Gps> result = manager.find(LocationFixture.WARD_KEY);

        assertThat(result).isPresent().contains(expected);
    }

    @Test
    @DisplayName("Redis에 값이 없으면 빈 Optional을 반환한다")
    void find_returns_empty_when_cache_missing() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("gps:latest:" + LocationFixture.WARD_KEY)).willReturn(null);

        Optional<Gps> result = manager.find(LocationFixture.WARD_KEY);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("역직렬화 실패 시 빈 Optional을 반환한다")
    void find_returns_empty_when_deserialization_fails() throws Exception {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("gps:latest:" + LocationFixture.WARD_KEY)).willReturn("invalid-json");
        given(objectMapper.readValue("invalid-json", Gps.class)).willThrow(com.fasterxml.jackson.core.JsonProcessingException.class);

        Optional<Gps> result = manager.find(LocationFixture.WARD_KEY);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("GPS 정보를 직렬화하여 Redis에 TTL 11분으로 저장한다")
    void save_serializes_and_stores_with_ttl() throws Exception {
        Gps gps = LocationFixture.createGps();
        given(objectMapper.writeValueAsString(gps)).willReturn("{\"lat\":37.5665}");
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        manager.save(LocationFixture.WARD_KEY, gps);

        then(valueOps).should(times(1))
                .set(eq("gps:latest:" + LocationFixture.WARD_KEY), anyString(), eq(11L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("직렬화 실패 시 예외를 전파하지 않는다")
    void save_does_not_propagate_serialization_failure() throws Exception {
        Gps gps = LocationFixture.createGps();
        given(objectMapper.writeValueAsString(gps)).willThrow(JsonProcessingException.class);

        assertThatCode(() -> manager.save(LocationFixture.WARD_KEY, gps)).doesNotThrowAnyException();

        then(redisTemplate).should(never()).opsForValue();
    }

    @Test
    @DisplayName("Redis 장애 시 예외를 전파하지 않는다")
    void save_does_not_propagate_redis_failure() throws Exception {
        Gps gps = LocationFixture.createGps();
        given(objectMapper.writeValueAsString(gps)).willReturn("{\"lat\":37.5665}");
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        willThrow(new RedisConnectionFailureException("connection refused"))
                .given(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        assertThatCode(() -> manager.save(LocationFixture.WARD_KEY, gps)).doesNotThrowAnyException();
    }
}
