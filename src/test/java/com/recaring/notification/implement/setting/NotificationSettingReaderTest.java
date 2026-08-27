package com.recaring.notification.implement.setting;

import com.recaring.location.vo.DetectionType;
import com.recaring.notification.vo.NotificationSettings;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.BatteryNotificationSetting;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 설정 조회 단위 테스트")
class NotificationSettingReaderTest {

    @InjectMocks
    private NotificationSettingReader notificationSettingReader;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Test
    @DisplayName("설정한 적이 없으면 모든 알림이 켜져 있고 배터리 임계값은 비어 있다")
    void findSetting_returns_all_enabled_when_never_configured() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        NotificationSettings result = notificationSettingReader.findSetting(NotificationFixture.WARD_KEY);

        assertThat(result.safeZone().entryEnabled()).isTrue();
        assertThat(result.safeZone().exitEnabled()).isTrue();
        assertThat(result.anomaly().speedAnomalyEnabled()).isTrue();
        assertThat(result.anomaly().wanderingAnomalyEnabled()).isTrue();
        assertThat(result.anomaly().abnormalDwellingEnabled()).isTrue();
        assertThat(result.anomaly().routeDeviationEnabled()).isTrue();
        assertThat(result.anomaly().timeAnomalyEnabled()).isTrue();
        assertThat(result.emergencyCall().enabled()).isTrue();
        assertThat(result.battery().lowBatteryEnabled()).isTrue();
        assertThat(result.battery().thresholdPercents()).isEmpty();
    }

    @Test
    @DisplayName("저장된 설정은 항목별 값을 그대로 돌려준다")
    void findSetting_returns_stored_values_per_item() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.of(NotificationFixture.createSettingWithAnomalyToggles(
                        NotificationFixture.WARD_KEY, false, true, false, true, true)));

        NotificationSettings result = notificationSettingReader.findSetting(NotificationFixture.WARD_KEY);

        assertThat(result.safeZone().entryEnabled()).isTrue();
        assertThat(result.safeZone().exitEnabled()).isFalse();
        assertThat(result.anomaly().speedAnomalyEnabled()).isFalse();
        assertThat(result.anomaly().wanderingAnomalyEnabled()).isTrue();
        assertThat(result.anomaly().abnormalDwellingEnabled()).isFalse();
        assertThat(result.anomaly().routeDeviationEnabled()).isTrue();
        assertThat(result.anomaly().timeAnomalyEnabled()).isTrue();
        assertThat(result.emergencyCall().enabled()).isTrue();
        assertThat(result.battery().lowBatteryEnabled()).isFalse();
        assertThat(result.battery().thresholdPercents()).containsExactly(40, 90);
    }

    @ParameterizedTest(name = "{0} 토글만 꺼두면 그 유형의 알림만 꺼진다")
    @CsvSource({
            "SPEED_ANOMALY,     false, true,  true,  true,  true",
            "WANDERING,         true,  false, true,  true,  true",
            "ABNORMAL_DWELLING, true,  true,  false, true,  true",
            "ROUTE_DEVIATION,   true,  true,  true,  false, true",
            "TIME_ANOMALY,      true,  true,  true,  true,  false",
    })
    @DisplayName("탐지 유형마다 자기 토글을 읽는다")
    void isAnomalyEnabled_reads_the_toggle_of_its_own_type(
            DetectionType detectionType,
            boolean speedEnabled,
            boolean wanderingEnabled,
            boolean abnormalDwellingEnabled,
            boolean routeDeviationEnabled,
            boolean timeAnomalyEnabled
    ) {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.of(NotificationFixture.createSettingWithAnomalyToggles(
                        NotificationFixture.WARD_KEY,
                        speedEnabled, wanderingEnabled, abnormalDwellingEnabled,
                        routeDeviationEnabled, timeAnomalyEnabled)));

        boolean result = notificationSettingReader.isAnomalyEnabled(NotificationFixture.WARD_KEY, detectionType);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("설정한 적이 없으면 이상탐지 알림은 켜진 것으로 본다")
    void isAnomalyEnabled_returns_true_when_never_configured() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        boolean result = notificationSettingReader.isAnomalyEnabled(
                NotificationFixture.WARD_KEY, DetectionType.WANDERING);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("설정한 적이 없으면 안심존 진입 알림은 켜진 것으로 본다")
    void isSafeZoneEntryEnabled_returns_true_when_never_configured() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        boolean result = notificationSettingReader.isSafeZoneEntryEnabled(NotificationFixture.WARD_KEY);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("저장된 안심존 진입 토글이 켜져 있으면 켜진 것으로 읽는다")
    void isSafeZoneEntryEnabled_reads_stored_toggle() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.of(NotificationFixture.createSetting(NotificationFixture.WARD_KEY)));

        boolean result = notificationSettingReader.isSafeZoneEntryEnabled(NotificationFixture.WARD_KEY);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("설정한 적이 없으면 안심존 이탈 알림은 켜진 것으로 본다")
    void isSafeZoneExitEnabled_returns_true_when_never_configured() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        boolean result = notificationSettingReader.isSafeZoneExitEnabled(NotificationFixture.WARD_KEY);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("저장된 안심존 이탈 토글이 꺼져 있으면 꺼진 것으로 읽는다")
    void isSafeZoneExitEnabled_reads_stored_toggle() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.of(NotificationFixture.createSetting(NotificationFixture.WARD_KEY)));

        boolean result = notificationSettingReader.isSafeZoneExitEnabled(NotificationFixture.WARD_KEY);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("설정을 반드시 찾아야 하는 조회는 행이 없으면 NOT_FOUND_DATA")
    void findExistingSetting_throws_when_absent() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationSettingReader.findExistingSetting(NotificationFixture.WARD_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_DATA);
    }

    @Test
    @DisplayName("설정을 반드시 찾아야 하는 조회는 저장된 설정을 그대로 돌려준다")
    void findExistingSetting_returns_stored_setting() {
        NotificationSetting expected = NotificationFixture.createSetting(NotificationFixture.WARD_KEY);
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.of(expected));

        NotificationSetting result = notificationSettingReader.findExistingSetting(NotificationFixture.WARD_KEY);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("배터리 스위치와 선택한 임계값을 한 번의 조회로 함께 돌려준다")
    void findBatterySetting_returns_switch_and_percents_in_one_lookup() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.of(NotificationFixture.createSetting(NotificationFixture.WARD_KEY)));

        BatteryNotificationSetting result =
                notificationSettingReader.findBatterySetting(NotificationFixture.WARD_KEY);

        assertThat(result.enabled()).isFalse();
        assertThat(result.thresholds().percents()).containsExactly(40, 90);
        then(notificationSettingRepository).should(times(1))
                .findByWardMemberKey(NotificationFixture.WARD_KEY);
    }

    @Test
    @DisplayName("설정한 적이 없으면 배터리 알림은 켜짐 + 임계값 없음이다")
    void findBatterySetting_returns_default_when_never_configured() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        BatteryNotificationSetting result =
                notificationSettingReader.findBatterySetting(NotificationFixture.WARD_KEY);

        assertThat(result.enabled()).isTrue();
        assertThat(result.hasNoThreshold()).isTrue();
    }
}
