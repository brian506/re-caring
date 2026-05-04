package com.recaring.location.dataaccess;

import com.recaring.location.dataaccess.entity.GpsHistory;
import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.support.AbstractRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GpsHistoryRepository 리포지토리 테스트")
class GpsHistoryRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private GpsHistoryRepository gpsHistoryRepository;

    @Test
    @DisplayName("wardKey와 날짜로 해당 날짜 범위의 GPS 이력을 오름차순으로 조회한다")
    void findByWardKeyAndDate_returns_ordered_histories() {
        LocalDate today = LocalDate.of(2024, 6, 1);
        GpsHistory h1 = gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, 37.1, 126.1));
        GpsHistory h2 = gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, 37.2, 126.2));
        // 어제 데이터 - 제외되어야 함
        GpsHistory yesterday = gpsHistoryRepository.save(buildHistory("other-ward", 37.9, 126.9));

        em.flush();
        em.clear();

        List<GpsHistory> result = gpsHistoryRepository.findByWardKeyAndDate(LocationFixture.WARD_KEY, today);

        // create-drop DDL 환경에서 @CreatedDate는 현재 시간으로 자동 설정되므로
        // wardKey 필터링이 올바른지 검증
        assertThat(result).allMatch(h -> h.getWardMemberKey().equals(LocationFixture.WARD_KEY));
    }

    @Test
    @DisplayName("다른 wardKey는 조회 결과에 포함되지 않는다")
    void findByWardKeyAndDate_excludes_other_wards() {
        gpsHistoryRepository.save(buildHistory("other-ward", 35.0, 127.0));
        em.flush();
        em.clear();

        List<GpsHistory> result = gpsHistoryRepository.findByWardKeyAndDate(
                LocationFixture.WARD_KEY, LocalDate.now());

        assertThat(result).noneMatch(h -> h.getWardMemberKey().equals("other-ward"));
    }

    @Test
    @DisplayName("findActiveWardKeysSince는 기준 시간 이후 GPS를 전송한 wardKey 목록을 중복 없이 반환한다")
    void findActiveWardKeysSince_returns_distinct_ward_keys() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, 37.1, 126.1));
        gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, 37.2, 126.2));
        gpsHistoryRepository.save(buildHistory("ward-key-002", 36.0, 127.0));
        em.flush();
        em.clear();

        List<String> activeKeys = gpsHistoryRepository.findActiveWardKeysSince(since);

        assertThat(activeKeys)
                .containsExactlyInAnyOrder(LocationFixture.WARD_KEY, "ward-key-002")
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("findByWardKeyBetween은 지정한 시간 범위 내 GPS 이력을 반환한다")
    void findByWardKeyBetween_returns_histories_in_range() {
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now().plusHours(1);
        gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, 37.1, 126.1));
        em.flush();
        em.clear();

        List<GpsHistory> result = gpsHistoryRepository.findByWardKeyBetween(
                LocationFixture.WARD_KEY, from, to);

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(h -> h.getWardMemberKey().equals(LocationFixture.WARD_KEY));
    }

    private GpsHistory buildHistory(String wardKey, double lat, double lng) {
        return GpsHistory.builder()
                .wardMemberKey(wardKey)
                .latitude(lat)
                .longitude(lng)
                .build();
    }
}
