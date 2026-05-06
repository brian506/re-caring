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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingManager 단위 테스트")
class NotificationSettingManagerTest {

    @InjectMocks
    private NotificationSettingManager notificationSettingManager;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Test
    @DisplayName("저장된 설정이 없으면 기본 설정을 생성하고 안심존 설정을 저장한다")
    void updateSafeZone_creates_default_setting_when_absent() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        notificationSettingManager.updateSafeZone(NotificationFixture.WARD_KEY, false, true);

        ArgumentCaptor<NotificationSetting> captor = ArgumentCaptor.forClass(NotificationSetting.class);
        then(notificationSettingRepository).should().save(captor.capture());
        assertThat(captor.getValue().getWardMemberKey()).isEqualTo(NotificationFixture.WARD_KEY);
        assertThat(captor.getValue().isSafeZoneEntryEnabled()).isFalse();
        assertThat(captor.getValue().isSafeZoneExitEnabled()).isTrue();
        assertThat(captor.getValue().getAnomalySensitivity()).isEqualTo(AnomalySensitivity.NORMAL);
        assertThat(captor.getValue().getBatteryThresholdPercent()).isEqualTo(25);
    }

    @Test
    @DisplayName("저장된 설정이 있으면 이상탐지 설정을 수정한다")
    void updateAnomaly_updates_existing_setting() {
        NotificationSetting setting = NotificationFixture.createSetting(NotificationFixture.WARD_KEY);
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.of(setting));

        notificationSettingManager.updateAnomaly(
                NotificationFixture.WARD_KEY,
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
    @DisplayName("저장된 설정이 있으면 배터리 알림 설정을 수정한다")
    void updateBattery_updates_existing_setting() {
        NotificationSetting setting = NotificationFixture.createSetting(NotificationFixture.WARD_KEY);
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.of(setting));

        notificationSettingManager.updateBattery(
                NotificationFixture.WARD_KEY,
                true,
                new BatteryThreshold(30)
        );

        then(notificationSettingRepository).should().save(setting);
        assertThat(setting.isLowBatteryEnabled()).isTrue();
        assertThat(setting.getBatteryThresholdPercent()).isEqualTo(30);
    }
}
