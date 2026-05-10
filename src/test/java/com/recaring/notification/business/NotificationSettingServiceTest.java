package com.recaring.notification.business;

import com.recaring.notification.business.command.UpdateAnomalyNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateBatteryNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateEmergencyCallNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateSafeZoneNotificationSettingCommand;
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
@DisplayName("NotificationSettingService 단위 테스트")
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
    @DisplayName("알림 설정 조회 시 접근 권한을 검증하고 설정 정보를 반환한다")
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
    @DisplayName("안심존 알림 설정 수정 시 접근 권한을 검증하고 설정을 저장한다")
    void updateSafeZone_validates_access_and_updates_setting() {
        UpdateSafeZoneNotificationSettingCommand command = new UpdateSafeZoneNotificationSettingCommand(
                NotificationFixture.WARD_KEY,
                false,
                true
        );

        notificationSettingService.updateSafeZone(NotificationFixture.GUARDIAN_KEY, command);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.GUARDIAN_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().updateSafeZone(NotificationFixture.WARD_KEY, false, true);
    }

    @Test
    @DisplayName("이상탐지 알림 설정 수정 시 접근 권한을 검증하고 설정을 저장한다")
    void updateAnomaly_validates_access_and_updates_setting() {
        UpdateAnomalyNotificationSettingCommand command = new UpdateAnomalyNotificationSettingCommand(
                NotificationFixture.WARD_KEY,
                true,
                false,
                true,
                AnomalySensitivity.HIGH
        );

        notificationSettingService.updateAnomaly(NotificationFixture.MANAGER_KEY, command);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.MANAGER_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().updateAnomaly(
                NotificationFixture.WARD_KEY,
                true,
                false,
                true,
                AnomalySensitivity.HIGH
        );
    }

    @Test
    @DisplayName("응급호출 알림 설정 수정 시 접근 권한을 검증하고 설정을 저장한다")
    void updateEmergencyCall_validates_access_and_updates_setting() {
        UpdateEmergencyCallNotificationSettingCommand command =
                new UpdateEmergencyCallNotificationSettingCommand(NotificationFixture.WARD_KEY, false);

        notificationSettingService.updateEmergencyCall(NotificationFixture.WARD_KEY, command);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should().updateEmergencyCall(NotificationFixture.WARD_KEY, false);
    }

    @Test
    @DisplayName("배터리 알림 설정 수정 시 접근 권한을 검증하고 설정을 저장한다")
    void updateBattery_validates_access_and_updates_setting() {
        UpdateBatteryNotificationSettingCommand command = new UpdateBatteryNotificationSettingCommand(
                NotificationFixture.WARD_KEY,
                true,
                new BatteryThreshold(30)
        );

        notificationSettingService.updateBattery(NotificationFixture.WARD_KEY, command);

        then(notificationSettingValidator).should()
                .validateSettingAccess(NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should()
                .updateBattery(NotificationFixture.WARD_KEY, true, new BatteryThreshold(30));
    }
}
