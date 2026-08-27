package com.recaring.location.dataaccess;

import com.recaring.location.dataaccess.entity.GpsHistory;
import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.support.AbstractRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@DisplayName("GpsHistoryRepository 리포지토리 테스트")
class GpsHistoryRepositoryTest extends AbstractRepositoryTest {

    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 7, 27);
    private static final String OTHER_WARD_KEY = "other-ward-member-key";

    @Autowired
    private GpsHistoryRepository gpsHistoryRepository;

    @Test
    @DisplayName("조회 날짜에 기록된 이력만 recordedAt 오름차순으로 반환한다")
    void findDailyGpsHistory_returns_target_date_histories_in_ascending_order() {
        LocalDateTime dayStart = TARGET_DATE.atStartOfDay();
        LocalDateTime evening = TARGET_DATE.atTime(18, 0);

        gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, evening));
        gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, dayStart));
        gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, dayStart.minusSeconds(1)));
        gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, TARGET_DATE.plusDays(1).atStartOfDay()));
        em.flush();
        em.clear();

        List<GpsHistory> result = gpsHistoryRepository.findDailyGpsHistory(LocationFixture.WARD_KEY, TARGET_DATE);

        assertThat(result)
                .extracting(GpsHistory::getRecordedAt)
                .containsExactly(dayStart, evening);
    }

    @Test
    @DisplayName("같은 날짜에 기록됐어도 다른 wardKey의 이력은 제외한다")
    void findDailyGpsHistory_excludes_other_wards() {
        LocalDateTime recordedAt = TARGET_DATE.atTime(9, 0);
        gpsHistoryRepository.save(buildHistory(LocationFixture.WARD_KEY, recordedAt));
        gpsHistoryRepository.save(buildHistory(OTHER_WARD_KEY, recordedAt));
        em.flush();
        em.clear();

        List<GpsHistory> result = gpsHistoryRepository.findDailyGpsHistory(LocationFixture.WARD_KEY, TARGET_DATE);

        assertThat(result)
                .extracting(GpsHistory::getWardMemberKey)
                .containsExactly(LocationFixture.WARD_KEY);
    }

    private GpsHistory buildHistory(String wardKey, LocalDateTime recordedAt) {
        return GpsHistory.builder()
                .wardMemberKey(wardKey)
                .latitude(LocationFixture.LATITUDE)
                .longitude(LocationFixture.LONGITUDE)
                .recordedAt(recordedAt)
                .build();
    }
}
