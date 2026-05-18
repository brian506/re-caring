package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.notification.vo.BatteryThreshold;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingManager unit test")
class NotificationSettingManagerTest {

    @InjectMocks
    private NotificationSettingManager notificationSettingManager;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Test
    @DisplayName("Creates a default setting row when absent")
    void addDefaultIfAbsent_inserts_default_setting() {
        notificationSettingManager.addDefaultIfAbsent(NotificationFixture.WARD_KEY);

        then(notificationSettingRepository).should().insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
    }

    @Test
    @DisplayName("Updates safe zone settings")
    void updateSafeZone_updates_setting() {
        NotificationSetting setting = NotificationFixture.createSetting(NotificationFixture.WARD_KEY);

        notificationSettingManager.updateSafeZone(setting, false, true);

        ArgumentCaptor<NotificationSetting> captor = ArgumentCaptor.forClass(NotificationSetting.class);
        then(notificationSettingRepository).should().save(captor.capture());
        assertThat(captor.getValue().getWardMemberKey()).isEqualTo(NotificationFixture.WARD_KEY);
        assertThat(captor.getValue().isSafeZoneEntryEnabled()).isFalse();
        assertThat(captor.getValue().isSafeZoneExitEnabled()).isTrue();
        assertThat(captor.getValue().getAnomalySensitivity()).isEqualTo(AnomalySensitivity.HIGH);
        assertThat(captor.getValue().getBatteryThresholdPercent()).isEqualTo(40);
    }

    @Test
    @DisplayName("Updates anomaly settings")
    void updateAnomaly_updates_existing_setting() {
        NotificationSetting setting = NotificationFixture.createSetting(NotificationFixture.WARD_KEY);

        notificationSettingManager.updateAnomaly(
                setting,
                false,
                true,
                false,
                AnomalySensitivity.LOW
        );

        then(notificationSettingRepository).should().save(setting);
        assertThat(setting.isRouteDeviationEnabled()).isFalse();
        assertThat(setting.isSpeedAnomalyEnabled()).isTrue();
        assertThat(setting.isWanderingAnomalyEnabled()).isFalse();
        assertThat(setting.getAnomalySensitivity()).isEqualTo(AnomalySensitivity.LOW);
    }

    @Test
    @DisplayName("Updates battery settings")
    void updateBattery_updates_existing_setting() {
        NotificationSetting setting = NotificationFixture.createSetting(NotificationFixture.WARD_KEY);

        notificationSettingManager.updateBattery(
                setting,
                true,
                new BatteryThreshold(30)
        );

        then(notificationSettingRepository).should().save(setting);
        assertThat(setting.isLowBatteryEnabled()).isTrue();
        assertThat(setting.getBatteryThresholdPercent()).isEqualTo(30);
    }
}
