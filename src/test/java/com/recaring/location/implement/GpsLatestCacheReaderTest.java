package com.recaring.location.implement;

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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GpsLatestCacheReader 단위 테스트")
class GpsLatestCacheReaderTest {

    @InjectMocks
    private GpsLatestCacheReader reader;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("Redis에 값이 있으면 역직렬화하여 Gps를 반환한다")
    void find_returns_gps_when_cache_exists() throws Exception {
        String json = "{\"lat\":37.5665,\"lng\":126.9780,\"recordedAt\":\"2024-01-01T00:00:00\"}";
        Gps expected = new Gps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE, LocalDateTime.now());
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("gps:latest:" + LocationFixture.WARD_KEY)).willReturn(json);
        given(objectMapper.readValue(json, Gps.class)).willReturn(expected);

        Optional<Gps> result = reader.find(LocationFixture.WARD_KEY);

        assertThat(result).isPresent().contains(expected);
    }

    @Test
    @DisplayName("Redis에 값이 없으면 빈 Optional을 반환한다")
    void find_returns_empty_when_cache_missing() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("gps:latest:" + LocationFixture.WARD_KEY)).willReturn(null);

        Optional<Gps> result = reader.find(LocationFixture.WARD_KEY);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("역직렬화 실패 시 빈 Optional을 반환한다")
    void find_returns_empty_when_deserialization_fails() throws Exception {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("gps:latest:" + LocationFixture.WARD_KEY)).willReturn("invalid-json");
        given(objectMapper.readValue("invalid-json", Gps.class)).willThrow(com.fasterxml.jackson.core.JsonProcessingException.class);

        Optional<Gps> result = reader.find(LocationFixture.WARD_KEY);

        assertThat(result).isEmpty();
    }
}
