package com.recaring.notification.implement;

import com.recaring.notification.business.NotificationSettingInfo;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingReader unit test")
class NotificationSettingReaderTest {

    @InjectMocks
    private NotificationSettingReader notificationSettingReader;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Test
    @DisplayName("Returns default notification settings when absent")
    void findSetting_returns_default_setting_when_absent() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        NotificationSettingInfo result = notificationSettingReader.findSetting(NotificationFixture.WARD_KEY);

        assertThat(result.safeZone().entryEnabled()).isTrue();
        assertThat(result.safeZone().exitEnabled()).isTrue();
        assertThat(result.anomaly().routeDeviationSensitivity()).isEqualTo("NORMAL");
        assertThat(result.anomaly().speedAnomalySensitivity()).isEqualTo("NORMAL");
        assertThat(result.anomaly().wanderingAnomalySensitivity()).isEqualTo("NORMAL");
        assertThat(result.emergencyCall().enabled()).isTrue();
        assertThat(result.battery().lowBatteryEnabled()).isTrue();
        assertThat(result.battery().lowThresholdPercent()).isEqualTo(25);
        assertThat(result.battery().fullThresholdPercent()).isEqualTo(100);
    }

    @Test
    @DisplayName("Returns saved notification settings")
    void findSetting_returns_saved_setting() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.of(NotificationFixture.createSetting(NotificationFixture.WARD_KEY)));

        NotificationSettingInfo result = notificationSettingReader.findSetting(NotificationFixture.WARD_KEY);

        assertThat(result.safeZone().entryEnabled()).isTrue();
        assertThat(result.safeZone().exitEnabled()).isFalse();
        assertThat(result.anomaly().routeDeviationEnabled()).isTrue();
        assertThat(result.anomaly().speedAnomalyEnabled()).isFalse();
        assertThat(result.anomaly().wanderingAnomalyEnabled()).isTrue();
        assertThat(result.anomaly().routeDeviationSensitivity()).isEqualTo("HIGH");
        assertThat(result.anomaly().speedAnomalySensitivity()).isEqualTo("LOW");
        assertThat(result.anomaly().wanderingAnomalySensitivity()).isEqualTo("VERY_HIGH");
        assertThat(result.emergencyCall().enabled()).isTrue();
        assertThat(result.battery().lowBatteryEnabled()).isFalse();
        assertThat(result.battery().lowThresholdPercent()).isEqualTo(40);
        assertThat(result.battery().fullThresholdPercent()).isEqualTo(90);
    }

    @Test
    @DisplayName("Returns existing notification setting entity")
    void findExistingSetting_returns_saved_setting_entity() {
        NotificationSetting expected = NotificationFixture.createSetting(NotificationFixture.WARD_KEY);
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.of(expected));

        NotificationSetting result = notificationSettingReader.findExistingSetting(NotificationFixture.WARD_KEY);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("Throws when existing notification setting entity is absent")
    void findExistingSetting_throws_when_setting_is_absent() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationSettingReader.findExistingSetting(NotificationFixture.WARD_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_DATA);
    }
}
