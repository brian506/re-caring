package com.recaring.notification.business;

import com.recaring.notification.business.command.UpdateAnomalyNotificationSettingCommand;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.setting.NotificationSettingManager;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import com.recaring.notification.implement.setting.NotificationSettingValidator;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.notification.vo.BatteryThresholds;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingService unit test")
class NotificationSettingServiceTest {

    @InjectMocks
    private NotificationSettingService notificationSettingService;

    @Mock
    private NotificationSettingReader notificationSettingReader;
    @Mock
    private NotificationSettingManager notificationSettingManager;
    @Mock
    private NotificationSettingValidator notificationSettingValidator;

    @Test
    @DisplayName("Validates access and returns notification settings")
    void getSetting_validates_access_and_returns_setting() {
        NotificationSettingInfo expected = NotificationSettingInfo.from(
                NotificationFixture.createSetting(NotificationFixture.WARD_KEY)
        );
        given(notificationSettingReader.findSetting(NotificationFixture.WARD_KEY)).willReturn(expected);

        notificationSettingService.getSetting(NotificationFixture.GUARDIAN_KEY, NotificationFixture.WARD_KEY);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.GUARDIAN_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingReader).should().findSetting(NotificationFixture.WARD_KEY);
    }

    @Test
    @DisplayName("Validates access and delegates safe zone update to the manager")
    void updateSafeZone_validates_access_and_updates_setting() {
        notificationSettingService.updateSafeZone(NotificationFixture.GUARDIAN_KEY, NotificationFixture.WARD_KEY, false, true);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.GUARDIAN_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().updateSafeZone(NotificationFixture.WARD_KEY, false, true);
    }

    @Test
    @DisplayName("Validates access and delegates anomaly update to the manager")
    void updateAnomaly_validates_access_and_updates_setting() {
        UpdateAnomalyNotificationSettingCommand command = new UpdateAnomalyNotificationSettingCommand(
                NotificationFixture.WARD_KEY,
                true,
                false,
                true,
                AnomalySensitivity.HIGH,
                AnomalySensitivity.LOW,
                AnomalySensitivity.VERY_HIGH
        );

        notificationSettingService.updateAnomaly(NotificationFixture.MANAGER_KEY, command);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.MANAGER_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().updateAnomaly(
                NotificationFixture.WARD_KEY,
                true,
                false,
                true,
                AnomalySensitivity.HIGH,
                AnomalySensitivity.LOW,
                AnomalySensitivity.VERY_HIGH
        );
    }

    @Test
    @DisplayName("Validates access and delegates emergency call update to the manager")
    void updateEmergencyCall_validates_access_and_updates_setting() {
        notificationSettingService.updateEmergencyCall(NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY, false);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().updateEmergencyCall(NotificationFixture.WARD_KEY, false);
    }

    @Test
    @DisplayName("Validates access and delegates battery update to the manager")
    void updateBattery_validates_access_and_updates_setting() {
        BatteryThresholds thresholds = BatteryThresholds.ofPercents(List.of(30, 90));

        notificationSettingService.updateBattery(
                NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY, true, thresholds);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should()
                .updateBattery(NotificationFixture.WARD_KEY, true, thresholds);
    }
}
