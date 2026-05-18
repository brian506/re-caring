package com.recaring.notification.business;

import com.recaring.notification.business.command.UpdateAnomalyNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateBatteryNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateEmergencyCallNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateSafeZoneNotificationSettingCommand;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.NotificationSettingManager;
import com.recaring.notification.implement.NotificationSettingReader;
import com.recaring.notification.implement.NotificationSettingValidator;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.notification.vo.BatteryThreshold;
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
    @DisplayName("Validates access, prepares a row, and updates safe zone settings")
    void updateSafeZone_validates_access_and_updates_setting() {
        UpdateSafeZoneNotificationSettingCommand command = new UpdateSafeZoneNotificationSettingCommand(
                NotificationFixture.WARD_KEY,
                false,
                true
        );
        NotificationSetting setting = NotificationFixture.createSetting(NotificationFixture.WARD_KEY);
        given(notificationSettingReader.findExistingSetting(NotificationFixture.WARD_KEY)).willReturn(setting);

        notificationSettingService.updateSafeZone(NotificationFixture.GUARDIAN_KEY, command);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.GUARDIAN_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().addDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingReader).should().findExistingSetting(NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().updateSafeZone(setting, false, true);
    }

    @Test
    @DisplayName("Validates access, prepares a row, and updates anomaly settings")
    void updateAnomaly_validates_access_and_updates_setting() {
        UpdateAnomalyNotificationSettingCommand command = new UpdateAnomalyNotificationSettingCommand(
                NotificationFixture.WARD_KEY,
                true,
                false,
                true,
                AnomalySensitivity.HIGH
        );
        NotificationSetting setting = NotificationFixture.createSetting(NotificationFixture.WARD_KEY);
        given(notificationSettingReader.findExistingSetting(NotificationFixture.WARD_KEY)).willReturn(setting);

        notificationSettingService.updateAnomaly(NotificationFixture.MANAGER_KEY, command);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.MANAGER_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().addDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingReader).should().findExistingSetting(NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().updateAnomaly(
                setting,
                true,
                false,
                true,
                AnomalySensitivity.HIGH
        );
    }

    @Test
    @DisplayName("Validates access, prepares a row, and updates emergency call settings")
    void updateEmergencyCall_validates_access_and_updates_setting() {
        UpdateEmergencyCallNotificationSettingCommand command =
                new UpdateEmergencyCallNotificationSettingCommand(NotificationFixture.WARD_KEY, false);
        NotificationSetting setting = NotificationFixture.createSetting(NotificationFixture.WARD_KEY);
        given(notificationSettingReader.findExistingSetting(NotificationFixture.WARD_KEY)).willReturn(setting);

        notificationSettingService.updateEmergencyCall(NotificationFixture.WARD_KEY, command);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().addDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingReader).should().findExistingSetting(NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().updateEmergencyCall(setting, false);
    }

    @Test
    @DisplayName("Validates access, prepares a row, and updates battery settings")
    void updateBattery_validates_access_and_updates_setting() {
        UpdateBatteryNotificationSettingCommand command = new UpdateBatteryNotificationSettingCommand(
                NotificationFixture.WARD_KEY,
                true,
                new BatteryThreshold(30)
        );
        NotificationSetting setting = NotificationFixture.createSetting(NotificationFixture.WARD_KEY);
        given(notificationSettingReader.findExistingSetting(NotificationFixture.WARD_KEY)).willReturn(setting);

        notificationSettingService.updateBattery(NotificationFixture.WARD_KEY, command);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().addDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingReader).should().findExistingSetting(NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should()
                .updateBattery(setting, true, new BatteryThreshold(30));
    }
}
