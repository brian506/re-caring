package com.recaring.notification.dataaccess;

import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.support.AbstractRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@DisplayName("NotificationSettingRepository 리포지토리 테스트")
class NotificationSettingRepositoryTest extends AbstractRepositoryTest {

    private static final String NO_BATTERY_THRESHOLDS = "";

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Test
    @DisplayName("insertDefaultIfAbsent는 키 컬럼만 지정해도 나머지는 스키마 DEFAULT로 채워 기본 행을 생성한다")
    void insertDefaultIfAbsent_creates_row_with_schema_defaults() {
        notificationSettingRepository.insertDefaultIfAbsent(NotificationFixture.WARD_KEY);

        NotificationSetting saved = notificationSettingRepository
                .findByWardMemberKey(NotificationFixture.WARD_KEY)
                .orElseThrow();

        assertThat(saved.isSafeZoneEntryEnabled()).isTrue();
        assertThat(saved.isSafeZoneExitEnabled()).isTrue();
        assertThat(saved.isRouteDeviationEnabled()).isTrue();
        assertThat(saved.isSpeedAnomalyEnabled()).isTrue();
        assertThat(saved.isWanderingAnomalyEnabled()).isTrue();
        assertThat(saved.getRouteDeviationSensitivity()).isEqualTo(AnomalySensitivity.NORMAL);
        assertThat(saved.getSpeedAnomalySensitivity()).isEqualTo(AnomalySensitivity.NORMAL);
        assertThat(saved.getWanderingAnomalySensitivity()).isEqualTo(AnomalySensitivity.NORMAL);
        assertThat(saved.isEmergencyCallEnabled()).isTrue();
        assertThat(saved.isLowBatteryEnabled()).isTrue();
        assertThat(saved.getBatteryThresholdPercents()).isEqualTo(NO_BATTERY_THRESHOLDS);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("insertDefaultIfAbsent를 두 번 호출해도 ON CONFLICT DO NOTHING으로 예외 없이 한 행만 유지된다")
    void insertDefaultIfAbsent_is_idempotent() {
        notificationSettingRepository.insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
        notificationSettingRepository.insertDefaultIfAbsent(NotificationFixture.WARD_KEY);

        assertThat(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY)).isPresent();
        assertThat(notificationSettingRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateSafeZone은 안심존 컬럼만 갱신하고 다른 컬럼은 그대로 둔다")
    void updateSafeZone_updates_only_safe_zone_columns() {
        notificationSettingRepository.insertDefaultIfAbsent(NotificationFixture.WARD_KEY);

        notificationSettingRepository.updateSafeZone(NotificationFixture.WARD_KEY, false, true);

        NotificationSetting updated = notificationSettingRepository
                .findByWardMemberKey(NotificationFixture.WARD_KEY)
                .orElseThrow();

        assertThat(updated.isSafeZoneEntryEnabled()).isFalse();
        assertThat(updated.isSafeZoneExitEnabled()).isTrue();
        // 나머지 컬럼은 건드리지 않았으므로 기본값 유지
        assertThat(updated.isRouteDeviationEnabled()).isTrue();
        assertThat(updated.isEmergencyCallEnabled()).isTrue();
        assertThat(updated.getBatteryThresholdPercents()).isEqualTo(NO_BATTERY_THRESHOLDS);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateAnomaly는 이상탐지 컬럼(enum 포함)만 갱신한다")
    void updateAnomaly_updates_only_anomaly_columns() {
        notificationSettingRepository.insertDefaultIfAbsent(NotificationFixture.WARD_KEY);

        notificationSettingRepository.updateAnomaly(
                NotificationFixture.WARD_KEY,
                false,
                true,
                false,
                AnomalySensitivity.HIGH,
                AnomalySensitivity.LOW,
                AnomalySensitivity.VERY_HIGH
        );

        NotificationSetting updated = notificationSettingRepository
                .findByWardMemberKey(NotificationFixture.WARD_KEY)
                .orElseThrow();

        assertThat(updated.isRouteDeviationEnabled()).isFalse();
        assertThat(updated.isSpeedAnomalyEnabled()).isTrue();
        assertThat(updated.isWanderingAnomalyEnabled()).isFalse();
        assertThat(updated.getRouteDeviationSensitivity()).isEqualTo(AnomalySensitivity.HIGH);
        assertThat(updated.getSpeedAnomalySensitivity()).isEqualTo(AnomalySensitivity.LOW);
        assertThat(updated.getWanderingAnomalySensitivity()).isEqualTo(AnomalySensitivity.VERY_HIGH);
        // 안심존/배터리 컬럼은 그대로
        assertThat(updated.isSafeZoneEntryEnabled()).isTrue();
        assertThat(updated.getBatteryThresholdPercents()).isEqualTo(NO_BATTERY_THRESHOLDS);
    }

    @Test
    @DisplayName("updateBattery는 배터리 컬럼만 갱신한다")
    void updateBattery_updates_only_battery_columns() {
        notificationSettingRepository.insertDefaultIfAbsent(NotificationFixture.WARD_KEY);

        notificationSettingRepository.updateBattery(NotificationFixture.WARD_KEY, false, "40,90");

        NotificationSetting updated = notificationSettingRepository
                .findByWardMemberKey(NotificationFixture.WARD_KEY)
                .orElseThrow();

        assertThat(updated.isLowBatteryEnabled()).isFalse();
        assertThat(updated.getBatteryThresholdPercents()).isEqualTo("40,90");
        assertThat(updated.isEmergencyCallEnabled()).isTrue();
    }
}
