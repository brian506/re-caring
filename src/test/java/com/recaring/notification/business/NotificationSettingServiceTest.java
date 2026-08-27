package com.recaring.notification.business;

import com.recaring.notification.business.command.UpdateAnomalyNotificationSettingCommand;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.NotificationSettings;
import com.recaring.notification.implement.setting.NotificationSettingManager;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import com.recaring.notification.implement.setting.NotificationSettingValidator;
import com.recaring.notification.vo.BatteryThresholds;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 설정 서비스 단위 테스트")
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
    @DisplayName("조회는 접근 권한을 확인한 뒤 대상자의 설정을 돌려준다")
    void getSetting_validates_access_and_returns_setting() {
        NotificationSettings expected = NotificationSettings.from(
                NotificationFixture.createSetting(NotificationFixture.WARD_KEY));
        given(notificationSettingReader.findSetting(NotificationFixture.WARD_KEY)).willReturn(expected);

        NotificationSettings result = notificationSettingService.getSetting(
                NotificationFixture.GUARDIAN_KEY, NotificationFixture.WARD_KEY);

        InOrder inOrder = inOrder(notificationSettingValidator, notificationSettingReader);
        then(notificationSettingValidator).should(inOrder)
                .validateSettingAccess(NotificationFixture.GUARDIAN_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingReader).should(inOrder).findSetting(NotificationFixture.WARD_KEY);
        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("케어 관계가 없는 요청자는 설정을 조회할 수 없다")
    void getSetting_throws_when_requester_is_not_care_related() {
        willThrow(new AppException(ErrorType.NOT_CARE_RELATED_WARD))
                .given(notificationSettingValidator)
                .validateSettingAccess(NotificationFixture.OTHER_GUARDIAN_KEY, NotificationFixture.WARD_KEY);

        assertThatThrownBy(() -> notificationSettingService.getSetting(
                NotificationFixture.OTHER_GUARDIAN_KEY, NotificationFixture.WARD_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_CARE_RELATED_WARD);
        then(notificationSettingReader).should(never()).findSetting(NotificationFixture.WARD_KEY);
    }

    @Test
    @DisplayName("안심존 설정 변경은 접근 권한을 확인한 뒤 위임한다")
    void updateSafeZone_validates_access_and_delegates() {
        notificationSettingService.updateSafeZone(
                NotificationFixture.GUARDIAN_KEY, NotificationFixture.WARD_KEY, false, true);

        InOrder inOrder = inOrder(notificationSettingValidator, notificationSettingManager);
        then(notificationSettingValidator).should(inOrder)
                .validateSettingAccess(NotificationFixture.GUARDIAN_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should(inOrder)
                .updateSafeZone(NotificationFixture.WARD_KEY, false, true);
    }

    @ParameterizedTest(name = "{0}번 토글만 끄면 그 자리만 false로 전달된다")
    @ValueSource(ints = {0, 1, 2, 3, 4})
    @DisplayName("이상탐지 토글 5개는 유형 순서를 유지한 채 각각 제자리로 전달된다")
    void updateAnomaly_passes_each_toggle_to_its_own_position(int disabledIndex) {
        // Given — 한 자리만 false로 두면 어느 두 자리를 뒤바꿔도 그 자리가 어긋난다
        boolean[] toggles = {true, true, true, true, true};
        toggles[disabledIndex] = false;
        UpdateAnomalyNotificationSettingCommand command = new UpdateAnomalyNotificationSettingCommand(
                NotificationFixture.WARD_KEY, toggles[0], toggles[1], toggles[2], toggles[3], toggles[4]);

        // When
        notificationSettingService.updateAnomaly(NotificationFixture.MANAGER_KEY, command);

        // Then
        InOrder inOrder = inOrder(notificationSettingValidator, notificationSettingManager);
        then(notificationSettingValidator).should(inOrder)
                .validateSettingAccess(NotificationFixture.MANAGER_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should(inOrder).updateAnomaly(
                NotificationFixture.WARD_KEY, toggles[0], toggles[1], toggles[2], toggles[3], toggles[4]);
    }

    @Test
    @DisplayName("응급 호출 설정 변경은 접근 권한을 확인한 뒤 위임한다")
    void updateEmergencyCall_validates_access_and_delegates() {
        notificationSettingService.updateEmergencyCall(
                NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY, false);

        InOrder inOrder = inOrder(notificationSettingValidator, notificationSettingManager);
        then(notificationSettingValidator).should(inOrder)
                .validateSettingAccess(NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should(inOrder)
                .updateEmergencyCall(NotificationFixture.WARD_KEY, false);
    }

    @Test
    @DisplayName("배터리 설정 변경은 고른 임계값을 그대로 넘긴다")
    void updateBattery_delegates_selected_thresholds() {
        BatteryThresholds thresholds = BatteryThresholds.ofPercents(List.of(30, 90));

        notificationSettingService.updateBattery(
                NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY, true, thresholds);

        InOrder inOrder = inOrder(notificationSettingValidator, notificationSettingManager);
        then(notificationSettingValidator).should(inOrder)
                .validateSettingAccess(NotificationFixture.WARD_KEY, NotificationFixture.WARD_KEY);
        then(notificationSettingManager).should(inOrder)
                .updateBattery(NotificationFixture.WARD_KEY, true, thresholds);
    }
}
