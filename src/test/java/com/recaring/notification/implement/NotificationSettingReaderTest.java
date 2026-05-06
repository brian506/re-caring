package com.recaring.notification.implement;

import com.recaring.notification.business.NotificationSettingInfo;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.fixture.NotificationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingReader 단위 테스트")
class NotificationSettingReaderTest {

    @InjectMocks
    private NotificationSettingReader notificationSettingReader;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Test
    @DisplayName("저장된 설정이 없으면 기본 알림 설정을 반환한다")
    void findSetting_returns_default_setting_when_absent() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        NotificationSettingInfo result = notificationSettingReader.findSetting(NotificationFixture.WARD_KEY);

        assertThat(result.safeZone().entryEnabled()).isTrue();
        assertThat(result.safeZone().exitEnabled()).isTrue();
        assertThat(result.anomaly().sensitivity()).isEqualTo("NORMAL");
        assertThat(result.emergencyCall().enabled()).isTrue();
        assertThat(result.battery().lowBatteryEnabled()).isTrue();
        assertThat(result.battery().thresholdPercent()).isEqualTo(25);
    }

    @Test
    @DisplayName("저장된 설정이 있으면 저장된 알림 설정을 반환한다")
    void findSetting_returns_saved_setting() {
        given(notificationSettingRepository.findByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(Optional.of(NotificationFixture.createSetting(NotificationFixture.WARD_KEY)));

        NotificationSettingInfo result = notificationSettingReader.findSetting(NotificationFixture.WARD_KEY);

        assertThat(result.safeZone().entryEnabled()).isTrue();
        assertThat(result.safeZone().exitEnabled()).isFalse();
        assertThat(result.anomaly().sensitivity()).isEqualTo("HIGH");
        assertThat(result.battery().thresholdPercent()).isEqualTo(40);
    }
}
