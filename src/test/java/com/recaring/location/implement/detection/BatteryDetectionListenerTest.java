package com.recaring.location.implement.detection;

import com.recaring.location.event.BatteryThresholdAlertEvent;
import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("배터리 잔량 판정 리스너 단위 테스트")
class BatteryDetectionListenerTest {

    private static final String GUARDIAN_KEY = LocationFixture.GUARDIAN_KEY;
    private static final String OTHER_GUARDIAN_KEY = LocationFixture.OTHER_GUARDIAN_KEY;

    @InjectMocks
    private BatteryDetectionListener batteryDetectionListener;

    @Mock
    private NotificationSettingReader notificationSettingReader;

    @Mock
    private BatteryAlertStateManager batteryAlertStateManager;

    @Spy
    private BatteryThresholdEvaluator batteryThresholdEvaluator = new BatteryThresholdEvaluator();

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<BatteryThresholdAlertEvent> eventCaptor;

    @Test
    @DisplayName("배터리 값이 없는 GPS는 판정하지 않는다")
    void onGpsSaved_skips_when_battery_absent() {
        batteryDetectionListener.onGpsSaved(gpsSavedEvent(null));

        then(notificationSettingReader).should(never()).findBatterySettings(anyString());
        then(batteryAlertStateManager).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("배터리 설정 행이 하나도 없으면 억제 상태를 읽지 않는다")
    void onGpsSaved_skips_state_lookup_when_no_setting_row() {
        given(notificationSettingReader.findBatterySettings(LocationFixture.WARD_KEY)).willReturn(Map.of());

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(15));

        then(batteryAlertStateManager).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림을 끈 보호자만 판정과 상태 갱신에서 빠지고 켜둔 보호자는 정상 판정된다")
    void onGpsSaved_excludes_caregiver_who_turned_off() {
        given(notificationSettingReader.findBatterySettings(LocationFixture.WARD_KEY)).willReturn(Map.of(
                GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(false, 50),
                OTHER_GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(true, 50)));
        given(batteryAlertStateManager.findAll(LocationFixture.WARD_KEY)).willReturn(Map.of());

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(45));

        then(batteryAlertStateManager).should(never()).save(eq(LocationFixture.WARD_KEY), eq(GUARDIAN_KEY), any());
        then(batteryAlertStateManager).should(never()).delete(LocationFixture.WARD_KEY, GUARDIAN_KEY);
        then(batteryAlertStateManager).should()
                .save(LocationFixture.WARD_KEY, OTHER_GUARDIAN_KEY, LocationFixture.batteryAlertState(50));
        then(eventPublisher).should(times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().recipientKeys()).containsExactly(OTHER_GUARDIAN_KEY);
    }

    @Test
    @DisplayName("고른 잔량이 없는 보호자는 그 보호자의 억제 상태만 지우고 알리지 않는다")
    void onGpsSaved_deletes_state_of_caregiver_without_threshold() {
        given(notificationSettingReader.findBatterySettings(LocationFixture.WARD_KEY)).willReturn(Map.of(
                GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(true)));
        given(batteryAlertStateManager.findAll(LocationFixture.WARD_KEY)).willReturn(Map.of());

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(15));

        then(batteryAlertStateManager).should(times(1)).delete(LocationFixture.WARD_KEY, GUARDIAN_KEY);
        then(batteryAlertStateManager).should(never()).delete(LocationFixture.WARD_KEY);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("잔량 45%에서 20%를 고른 보호자는 알림을 받지 않고 50%를 고른 보호자만 50% 알림을 받는다")
    void onGpsSaved_judges_each_caregiver_by_own_thresholds() {
        given(notificationSettingReader.findBatterySettings(LocationFixture.WARD_KEY)).willReturn(Map.of(
                GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(true, 20),
                OTHER_GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(true, 50)));
        given(batteryAlertStateManager.findAll(LocationFixture.WARD_KEY)).willReturn(Map.of());

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(45));

        then(eventPublisher).should(times(1)).publishEvent(eventCaptor.capture());
        BatteryThresholdAlertEvent published = eventCaptor.getValue();
        assertThat(published.memberKey()).isEqualTo(LocationFixture.WARD_KEY);
        assertThat(published.thresholdPercent()).isEqualTo(50);
        assertThat(published.recipientKeys()).containsExactly(OTHER_GUARDIAN_KEY);
        assertThat(published.detectedAt()).isEqualTo(LocationFixture.MEASURED_AT);
        then(batteryAlertStateManager).should()
                .save(LocationFixture.WARD_KEY, OTHER_GUARDIAN_KEY, LocationFixture.batteryAlertState(50));
    }

    @Test
    @DisplayName("서로 다른 잔량에 도달한 보호자들에게는 도달한 잔량마다 알림이 따로 나간다")
    void onGpsSaved_publishes_separate_event_per_reached_threshold() {
        given(notificationSettingReader.findBatterySettings(LocationFixture.WARD_KEY)).willReturn(Map.of(
                GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(true, 20),
                OTHER_GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(true, 50)));
        given(batteryAlertStateManager.findAll(LocationFixture.WARD_KEY)).willReturn(Map.of());

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(15));

        then(eventPublisher).should(times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(BatteryThresholdAlertEvent::thresholdPercent, BatteryThresholdAlertEvent::recipientKeys)
                .containsExactlyInAnyOrder(
                        tuple(20, List.of(GUARDIAN_KEY)),
                        tuple(50, List.of(OTHER_GUARDIAN_KEY)));
    }

    @Test
    @DisplayName("같은 잔량에 도달한 보호자들은 알림 하나로 묶인다")
    void onGpsSaved_groups_caregivers_reaching_same_threshold() {
        given(notificationSettingReader.findBatterySettings(LocationFixture.WARD_KEY)).willReturn(Map.of(
                GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(true, 50),
                OTHER_GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(true, 50, 30)));
        given(batteryAlertStateManager.findAll(LocationFixture.WARD_KEY)).willReturn(Map.of());

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(45));

        then(eventPublisher).should(times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().thresholdPercent()).isEqualTo(50);
        assertThat(eventCaptor.getValue().recipientKeys())
                .containsExactlyInAnyOrder(GUARDIAN_KEY, OTHER_GUARDIAN_KEY);
    }

    @Test
    @DisplayName("이미 알림을 받은 보호자에게는 다시 보내지 않고 아직 받지 않은 보호자에게는 보낸다")
    void onGpsSaved_keeps_suppression_per_caregiver() {
        given(notificationSettingReader.findBatterySettings(LocationFixture.WARD_KEY)).willReturn(Map.of(
                GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(true, 50),
                OTHER_GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(true, 50)));
        given(batteryAlertStateManager.findAll(LocationFixture.WARD_KEY))
                .willReturn(Map.of(GUARDIAN_KEY, LocationFixture.batteryAlertState(50)));

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(45));

        then(eventPublisher).should(times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().recipientKeys()).containsExactly(OTHER_GUARDIAN_KEY);
    }

    @Test
    @DisplayName("알릴 것이 없어도 판정 결과 상태는 저장한다")
    void onGpsSaved_saves_state_without_publishing_when_silent() {
        given(notificationSettingReader.findBatterySettings(LocationFixture.WARD_KEY)).willReturn(Map.of(
                GUARDIAN_KEY, NotificationFixture.batteryNotificationSetting(true, 50)));
        given(batteryAlertStateManager.findAll(LocationFixture.WARD_KEY))
                .willReturn(Map.of(GUARDIAN_KEY, LocationFixture.batteryAlertState(50)));

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(44));

        then(batteryAlertStateManager).should(times(1))
                .save(LocationFixture.WARD_KEY, GUARDIAN_KEY, LocationFixture.batteryAlertState(50));
        then(eventPublisher).shouldHaveNoInteractions();
    }

    private GpsSavedEvent gpsSavedEvent(Integer battery) {
        return new GpsSavedEvent(LocationFixture.WARD_KEY, LocationFixture.createGps(battery));
    }
}
