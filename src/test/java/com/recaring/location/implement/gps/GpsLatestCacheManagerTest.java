package com.recaring.location.implement.gps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.config.mapper.JacksonConfig;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.Gps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import org.springframework.data.redis.RedisConnectionFailureException;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThatCode;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    @Spy
    private ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("Redis에 값이 있으면 역직렬화하여 Gps를 반환한다")
    void find_returns_gps_when_cache_exists() throws Exception {
        Gps expected = LocationFixture.createGps();
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("gps:latest:" + LocationFixture.WARD_KEY))
                .willReturn(new JacksonConfig().objectMapper().writeValueAsString(expected));

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

        Optional<Gps> result = manager.find(LocationFixture.WARD_KEY);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("GPS 정보를 직렬화하여 Redis에 TTL 25시간으로 저장한다")
    void save_serializes_and_stores_with_ttl() throws Exception {
        Gps gps = LocationFixture.createGps();
        String expectedJson = new JacksonConfig().objectMapper().writeValueAsString(gps);
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        manager.save(LocationFixture.WARD_KEY, gps);

        then(valueOps).should(times(1))
                .set("gps:latest:" + LocationFixture.WARD_KEY, expectedJson, 25L, TimeUnit.HOURS);
    }

    @Test
    @DisplayName("직렬화 실패 시 예외를 전파하지 않는다")
    void save_does_not_propagate_serialization_failure() throws Exception {
        Gps gps = LocationFixture.createGps();
        willThrow(JsonProcessingException.class).given(objectMapper).writeValueAsString(gps);

        assertThatCode(() -> manager.save(LocationFixture.WARD_KEY, gps)).doesNotThrowAnyException();

        then(redisTemplate).should(never()).opsForValue();
    }

    @Test
    @DisplayName("Redis 장애 시 예외를 전파하지 않는다")
    void save_does_not_propagate_redis_failure() throws Exception {
        Gps gps = LocationFixture.createGps();
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        willThrow(new RedisConnectionFailureException("connection refused"))
                .given(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        assertThatCode(() -> manager.save(LocationFixture.WARD_KEY, gps)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("여러 대상자를 한 번에 조회하면 대상자마다 자기 위치가 짝지어진다")
    void findAll_pairs_each_ward_with_its_own_gps() throws Exception {
        ObjectMapper realMapper = new JacksonConfig().objectMapper();
        GpsLatestCacheManager realMapperManager = new GpsLatestCacheManager(redisTemplate, realMapper);
        Gps wardGps = LocationFixture.createGps();
        Gps otherWardGps = LocationFixture.createGpsReceivedAt(LocationFixture.RECORDED_AT.plusMinutes(5));
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.multiGet(List.of(
                "gps:latest:" + LocationFixture.WARD_KEY,
                "gps:latest:" + LocationFixture.OTHER_WARD_KEY)))
                .willReturn(List.of(realMapper.writeValueAsString(wardGps), realMapper.writeValueAsString(otherWardGps)));

        Map<String, Gps> result = realMapperManager.findAll(
                List.of(LocationFixture.WARD_KEY, LocationFixture.OTHER_WARD_KEY));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                LocationFixture.WARD_KEY, wardGps,
                LocationFixture.OTHER_WARD_KEY, otherWardGps));
    }

    @Test
    @DisplayName("캐시에 없는 대상자와 역직렬화에 실패한 값은 결과에서 빠지고 나머지는 유지된다")
    void findAll_skips_missing_and_malformed_values() throws Exception {
        ObjectMapper realMapper = new JacksonConfig().objectMapper();
        GpsLatestCacheManager realMapperManager = new GpsLatestCacheManager(redisTemplate, realMapper);
        Gps wardGps = LocationFixture.createGps();
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.multiGet(List.of(
                "gps:latest:" + LocationFixture.WARD_KEY,
                "gps:latest:" + LocationFixture.OTHER_WARD_KEY,
                "gps:latest:" + LocationFixture.THIRD_WARD_KEY)))
                .willReturn(Arrays.asList(realMapper.writeValueAsString(wardGps), null, "invalid-json"));

        Map<String, Gps> result = realMapperManager.findAll(List.of(
                LocationFixture.WARD_KEY, LocationFixture.OTHER_WARD_KEY, LocationFixture.THIRD_WARD_KEY));

        assertThat(result).containsExactlyEntriesOf(Map.of(LocationFixture.WARD_KEY, wardGps));
    }

    @Test
    @DisplayName("표시용 위치는 원본 최신 위치와 다른 키에 저장된다")
    void displayable_is_stored_under_its_own_key() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        manager.saveDisplayable(LocationFixture.WARD_KEY, LocationFixture.createGps());

        then(valueOps).should()
                .set(eq("gps:display:" + LocationFixture.WARD_KEY), anyString(), eq(25L), eq(TimeUnit.HOURS));
        then(valueOps).should(never())
                .set(eq("gps:latest:" + LocationFixture.WARD_KEY), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("탈퇴 정리 시 원본 최신 위치와 표시용 위치를 모두 지운다")
    @SuppressWarnings("unchecked")
    void delete_removes_both_raw_and_displayable_keys() {
        ArgumentCaptor<Collection<String>> keys = ArgumentCaptor.forClass(Collection.class);

        manager.delete(LocationFixture.WARD_KEY);

        then(redisTemplate).should().delete(keys.capture());
        assertThat(keys.getValue()).containsExactlyInAnyOrder(
                "gps:latest:" + LocationFixture.WARD_KEY,
                "gps:display:" + LocationFixture.WARD_KEY);
    }
}
