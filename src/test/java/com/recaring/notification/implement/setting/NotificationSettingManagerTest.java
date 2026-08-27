package com.recaring.notification.implement.setting;

import com.recaring.location.implement.detection.BatteryAlertStateManager;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.BatteryThresholds;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 설정 변경 단위 테스트")
class NotificationSettingManagerTest {

    @InjectMocks
    private NotificationSettingManager notificationSettingManager;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private BatteryAlertStateManager batteryAlertStateManager;

    @Test
    @DisplayName("설정한 적 없는 대상자에게 기본 설정 행을 만들어 둔다")
    void addDefaultIfAbsent_creates_default_row() {
        notificationSettingManager.addDefaultIfAbsent(NotificationFixture.WARD_KEY);

        then(notificationSettingRepository).should().insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
    }

    @Test
    @DisplayName("안심존 설정은 행을 먼저 확보한 뒤 갱신한다")
    void updateSafeZone_ensures_row_before_updating() {
        notificationSettingManager.updateSafeZone(NotificationFixture.WARD_KEY, false, true);

        InOrder inOrder = inOrder(notificationSettingRepository);
        then(notificationSettingRepository).should(inOrder).insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingRepository).should(inOrder).updateSafeZone(NotificationFixture.WARD_KEY, false, true);
    }

    @Test
    @DisplayName("이상탐지 설정은 토글 5개를 유형 순서 그대로 갱신한다")
    void updateAnomaly_passes_five_toggles_in_order() {
        notificationSettingManager.updateAnomaly(
                NotificationFixture.WARD_KEY, false, true, false, true, false);

        InOrder inOrder = inOrder(notificationSettingRepository);
        then(notificationSettingRepository).should(inOrder).insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingRepository).should(inOrder).updateAnomaly(
                NotificationFixture.WARD_KEY, false, true, false, true, false);
    }

    @Test
    @DisplayName("응급 호출 설정은 행을 먼저 확보한 뒤 갱신한다")
    void updateEmergencyCall_ensures_row_before_updating() {
        notificationSettingManager.updateEmergencyCall(NotificationFixture.WARD_KEY, false);

        InOrder inOrder = inOrder(notificationSettingRepository);
        then(notificationSettingRepository).should(inOrder).insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingRepository).should(inOrder).updateEmergencyCall(NotificationFixture.WARD_KEY, false);
    }

    @Test
    @DisplayName("배터리 알림을 켜면 고른 임계값을 저장하고 기존 발송 이력은 남긴다")
    void updateBattery_keeps_alert_state_when_enabled() {
        notificationSettingManager.updateBattery(
                NotificationFixture.WARD_KEY, true, BatteryThresholds.ofPercents(List.of(30, 90)));

        InOrder inOrder = inOrder(notificationSettingRepository);
        then(notificationSettingRepository).should(inOrder).insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingRepository).should(inOrder)
                .updateBattery(NotificationFixture.WARD_KEY, true, "30,90");
        then(batteryAlertStateManager).should(never()).delete(NotificationFixture.WARD_KEY);
    }

    @Test
    @DisplayName("배터리 알림을 끄면 마지막 발송 이력도 함께 지운다")
    void updateBattery_clears_alert_state_when_disabled() {
        notificationSettingManager.updateBattery(
                NotificationFixture.WARD_KEY, false, BatteryThresholds.ofPercents(List.of(30, 90)));

        InOrder inOrder = inOrder(notificationSettingRepository);
        then(notificationSettingRepository).should(inOrder).insertDefaultIfAbsent(NotificationFixture.WARD_KEY);
        then(notificationSettingRepository).should(inOrder)
                .updateBattery(NotificationFixture.WARD_KEY, false, "30,90");
        then(batteryAlertStateManager).should().delete(NotificationFixture.WARD_KEY);
    }
}
