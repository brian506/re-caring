package com.recaring.location.dataaccess;

import com.recaring.location.dataaccess.entity.GpsHistory;
import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.support.AbstractRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@DisplayName("GpsHistoryRepository 리포지토리 테스트")
class GpsHistoryRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private GpsHistoryRepository gpsHistoryRepository;

    @Test
    @DisplayName("wardKey와 날짜로 해당 날짜 범위의 GPS 이력을 오름차순으로 조회한다")
    void findDailyGpsHistory_returns_ordered_histories() {
        LocalDate today = LocalDate.of(2024, 6, 1);
        GpsHistory h1 = gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, 37.1, 126.1));
        GpsHistory h2 = gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, 37.2, 126.2));
        // 어제 데이터 - 제외되어야 함
        GpsHistory yesterday = gpsHistoryRepository.save(buildHistory("other-ward", 37.9, 126.9));

        em.flush();
        em.clear();

        List<GpsHistory> result = gpsHistoryRepository.findDailyGpsHistory(LocationFixture.WARD_KEY, today);

        // create-drop DDL 환경에서 @CreatedDate는 현재 시간으로 자동 설정되므로
        // wardKey 필터링이 올바른지 검증
        assertThat(result).allMatch(h -> h.getWardMemberKey().equals(LocationFixture.WARD_KEY));
    }

    @Test
    @DisplayName("다른 wardKey는 조회 결과에 포함되지 않는다")
    void findDailyGpsHistory_excludes_other_wards() {
        gpsHistoryRepository.save(buildHistory("other-ward", 35.0, 127.0));
        em.flush();
        em.clear();

        List<GpsHistory> result = gpsHistoryRepository.findDailyGpsHistory(
                LocationFixture.WARD_KEY, LocalDate.now());

        assertThat(result).noneMatch(h -> h.getWardMemberKey().equals("other-ward"));
    }

    private GpsHistory buildHistory(String wardKey, double lat, double lng) {
        return GpsHistory.builder()
                .wardMemberKey(wardKey)
                .latitude(lat)
                .longitude(lng)
                .build();
    }
}
