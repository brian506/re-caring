package com.recaring.location.implement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.Gps;
import com.recaring.support.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GpsLatestCacheWriter 단위 테스트")
class GpsLatestCacheWriterTest {

    @InjectMocks
    private GpsLatestCacheWriter writer;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("GPS 정보를 직렬화하여 Redis에 TTL 5분으로 저장한다")
    void save_serializes_and_stores_with_ttl() throws Exception {
        Gps gps = new Gps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE, LocalDateTime.now());
        given(objectMapper.writeValueAsString(gps)).willReturn("{\"lat\":37.5665}");
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        writer.save(LocationFixture.WARD_KEY, gps);

        then(valueOps).should(times(1))
                .set(eq("gps:latest:" + LocationFixture.WARD_KEY), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("직렬화 실패 시 AppException이 발생한다")
    void save_throws_app_exception_on_serialization_failure() throws Exception {
        Gps gps = new Gps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE, LocalDateTime.now());
        given(objectMapper.writeValueAsString(gps)).willThrow(JsonProcessingException.class);

        assertThatThrownBy(() -> writer.save(LocationFixture.WARD_KEY, gps))
                .isInstanceOf(AppException.class);
    }
}
