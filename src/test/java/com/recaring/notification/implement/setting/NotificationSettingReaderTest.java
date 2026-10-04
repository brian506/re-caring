package com.recaring.notification.implement.setting;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.location.vo.DetectionType;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.BatteryNotificationSetting;
import com.recaring.notification.vo.NotificationSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 설정 조회 단위 테스트")
class NotificationSettingReaderTest {

    private static final CaregiverInfo GUARDIAN =
            CareFixture.createCaregiverInfo(NotificationFixture.GUARDIAN_KEY, CareRole.PRIMARY_GUARDIAN);
    private static final CaregiverInfo OTHER_GUARDIAN =
            CareFixture.createCaregiverInfo(NotificationFixture.OTHER_GUARDIAN_KEY, CareRole.GUARDIAN);
    private static final CaregiverInfo MANAGER =
            CareFixture.createCaregiverInfo(NotificationFixture.MANAGER_KEY, CareRole.MANAGER);

    @InjectMocks
    private NotificationSettingReader notificationSettingReader;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Test
    @DisplayName("설정한 적 없는 보호자는 모든 알림이 켜져 있고 배터리 임계값은 비어 있다")
    void findSetting_returns_all_enabled_when_never_configured() {
        given(notificationSettingRepository.findByWardMemberKeyAndCaregiverMemberKey(
                NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY))
                .willReturn(Optional.empty());

        NotificationSettings result = notificationSettingReader.findSetting(
                NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY);

        assertThat(result.safeZone().entryEnabled()).isTrue();
        assertThat(result.safeZone().exitEnabled()).isTrue();
        assertThat(result.dangerZone().entryEnabled()).isTrue();
        assertThat(result.dangerZone().exitEnabled()).isTrue();
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
    @DisplayName("저장된 설정은 요청한 보호자의 항목별 값을 그대로 돌려준다")
    void findSetting_returns_stored_values_of_the_caregiver() {
        given(notificationSettingRepository.findByWardMemberKeyAndCaregiverMemberKey(
                NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY))
                .willReturn(Optional.of(NotificationFixture.createSettingWithAnomalyToggles(
                        NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY,
                        false, true, false, true, true)));

        NotificationSettings result = notificationSettingReader.findSetting(
                NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY);

        assertThat(result.safeZone().entryEnabled()).isTrue();
        assertThat(result.safeZone().exitEnabled()).isFalse();
        assertThat(result.dangerZone().entryEnabled()).isTrue();
        assertThat(result.dangerZone().exitEnabled()).isFalse();
        assertThat(result.anomaly().speedAnomalyEnabled()).isFalse();
        assertThat(result.anomaly().wanderingAnomalyEnabled()).isTrue();
        assertThat(result.anomaly().abnormalDwellingEnabled()).isFalse();
        assertThat(result.anomaly().routeDeviationEnabled()).isTrue();
        assertThat(result.anomaly().timeAnomalyEnabled()).isTrue();
        assertThat(result.emergencyCall().enabled()).isTrue();
        assertThat(result.battery().lowBatteryEnabled()).isFalse();
        assertThat(result.battery().thresholdPercents()).containsExactly(40, 90);
    }

    @Test
    @DisplayName("안심존 진입 알림을 끈 보호자만 수신자에서 빠지고 나머지는 남는다")
    void filterRecipients_excludes_only_caregiver_who_turned_off_entry() {
        given(notificationSettingRepository.findAllByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        NotificationFixture.createSafeZoneSetting(
                                NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY, false, true),
                        NotificationFixture.createSafeZoneSetting(
                                NotificationFixture.WARD_KEY, NotificationFixture.OTHER_GUARDIAN_KEY, true, true)));

        List<CaregiverInfo> result = notificationSettingReader.filterRecipients(
                NotificationFixture.WARD_KEY,
                List.of(GUARDIAN, OTHER_GUARDIAN),
                settings -> settings.safeZone().entryEnabled());

        assertThat(result).containsExactly(OTHER_GUARDIAN);
    }

    @Test
    @DisplayName("설정 행이 없는 보호자는 수신자로 남는다")
    void filterRecipients_keeps_caregiver_without_setting_row() {
        given(notificationSettingRepository.findAllByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(List.of(NotificationFixture.createSafeZoneSetting(
                        NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY, false, false)));

        List<CaregiverInfo> result = notificationSettingReader.filterRecipients(
                NotificationFixture.WARD_KEY,
                List.of(GUARDIAN, MANAGER),
                settings -> settings.safeZone().entryEnabled());

        assertThat(result).containsExactly(MANAGER);
    }

    @Test
    @DisplayName("진입은 켜고 이탈만 끈 보호자는 이탈 알림 수신자에서만 빠진다")
    void filterRecipients_reads_exit_toggle_separately_from_entry() {
        given(notificationSettingRepository.findAllByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(List.of(NotificationFixture.createSafeZoneSetting(
                        NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY, true, false)));

        List<CaregiverInfo> result = notificationSettingReader.filterRecipients(
                NotificationFixture.WARD_KEY,
                List.of(GUARDIAN),
                settings -> settings.safeZone().exitEnabled());

        assertThat(result).isEmpty();
    }

    @ParameterizedTest(name = "{0} 토글만 꺼둔 보호자는 그 유형의 알림 수신자에서 빠진다")
    @CsvSource({
            "SPEED_ANOMALY,     false, true,  true,  true,  true",
            "WANDERING,         true,  false, true,  true,  true",
            "ABNORMAL_DWELLING, true,  true,  false, true,  true",
            "ROUTE_DEVIATION,   true,  true,  true,  false, true",
            "TIME_ANOMALY,      true,  true,  true,  true,  false",
    })
    @DisplayName("이상탐지는 탐지 유형마다 그 유형의 토글로 거른다")
    void filterRecipients_reads_the_anomaly_toggle_of_its_own_type(
            DetectionType detectionType,
            boolean speedEnabled,
            boolean wanderingEnabled,
            boolean abnormalDwellingEnabled,
            boolean routeDeviationEnabled,
            boolean timeAnomalyEnabled
    ) {
        given(notificationSettingRepository.findAllByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(List.of(NotificationFixture.createSettingWithAnomalyToggles(
                        NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY,
                        speedEnabled, wanderingEnabled, abnormalDwellingEnabled,
                        routeDeviationEnabled, timeAnomalyEnabled)));

        List<CaregiverInfo> result = notificationSettingReader.filterRecipients(
                NotificationFixture.WARD_KEY,
                List.of(GUARDIAN, OTHER_GUARDIAN),
                settings -> settings.anomaly().isEnabled(detectionType));

        assertThat(result).containsExactly(OTHER_GUARDIAN);
    }

    @Test
    @DisplayName("배터리 설정은 행이 있는 보호자만 보호자별 스위치와 임계값으로 돌려준다")
    void findBatterySettings_returns_settings_per_caregiver() {
        given(notificationSettingRepository.findAllByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        NotificationFixture.createBatterySetting(
                                NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY, true, "20"),
                        NotificationFixture.createBatterySetting(
                                NotificationFixture.WARD_KEY, NotificationFixture.OTHER_GUARDIAN_KEY, false, "50,30")));

        Map<String, BatteryNotificationSetting> result =
                notificationSettingReader.findBatterySettings(NotificationFixture.WARD_KEY);

        assertThat(result).containsOnlyKeys(NotificationFixture.GUARDIAN_KEY, NotificationFixture.OTHER_GUARDIAN_KEY);
        assertThat(result.get(NotificationFixture.GUARDIAN_KEY).enabled()).isTrue();
        assertThat(result.get(NotificationFixture.GUARDIAN_KEY).thresholds().percents()).containsExactly(20);
        assertThat(result.get(NotificationFixture.OTHER_GUARDIAN_KEY).enabled()).isFalse();
        assertThat(result.get(NotificationFixture.OTHER_GUARDIAN_KEY).thresholds().percents())
                .containsExactlyInAnyOrder(50, 30);
    }
}
