package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.notification.vo.BatteryThreshold;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @DisplayName("Ensures the row exists, then updates only the safe zone columns")
    void updateSafeZone_updates_setting() {
        notificationSettingManager.updateSafeZone(NotificationFixture.WARD_KEY, false, true);

        then(notificationSettingRepository).should().insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingRepository).should().updateSafeZone(NotificationFixture.WARD_KEY, false, true);
    }

    @Test
    @DisplayName("Ensures the row exists, then updates only the anomaly columns")
    void updateAnomaly_updates_setting() {
        notificationSettingManager.updateAnomaly(
                NotificationFixture.WARD_KEY,
                false,
                true,
                false,
                AnomalySensitivity.LOW,
                AnomalySensitivity.VERY_HIGH,
                AnomalySensitivity.VERY_LOW
        );

        then(notificationSettingRepository).should().insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingRepository).should().updateAnomaly(
                NotificationFixture.WARD_KEY,
                false,
                true,
                false,
                AnomalySensitivity.LOW,
                AnomalySensitivity.VERY_HIGH,
                AnomalySensitivity.VERY_LOW
        );
    }

    @Test
    @DisplayName("Ensures the row exists, then updates only the emergency call column")
    void updateEmergencyCall_updates_setting() {
        notificationSettingManager.updateEmergencyCall(NotificationFixture.WARD_KEY, false);

        then(notificationSettingRepository).should().insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingRepository).should().updateEmergencyCall(NotificationFixture.WARD_KEY, false);
    }

    @Test
    @DisplayName("Ensures the row exists, then updates only the battery columns")
    void updateBattery_updates_setting() {
        notificationSettingManager.updateBattery(
                NotificationFixture.WARD_KEY,
                true,
                new BatteryThreshold(30)
        );

        then(notificationSettingRepository).should().insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingRepository).should().updateBattery(NotificationFixture.WARD_KEY, true, 30);
    }
}
