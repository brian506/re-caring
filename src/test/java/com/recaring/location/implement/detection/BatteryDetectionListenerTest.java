package com.recaring.location.implement.detection;

import com.recaring.location.event.BatteryThresholdAlertEvent;
import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.BatteryAlertState;
import com.recaring.location.vo.BatteryEvaluation;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import com.recaring.notification.vo.BatteryNotificationSetting;
import com.recaring.notification.vo.BatteryThresholds;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatteryDetectionListener 단위 테스트")
class BatteryDetectionListenerTest {

    private static final List<Integer> THRESHOLD_PERCENTS = List.of(20, 50);

    @InjectMocks
    private BatteryDetectionListener batteryDetectionListener;

    @Mock
    private NotificationSettingReader notificationSettingReader;

    @Mock
    private BatteryAlertStateManager batteryAlertStateManager;

    @Mock
    private BatteryThresholdEvaluator batteryThresholdEvaluator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<BatteryThresholdAlertEvent> eventCaptor;

    @Test
    @DisplayName("배터리 값이 없는 GPS는 판정하지 않는다")
    void onGpsSaved_skips_when_battery_absent() {
        batteryDetectionListener.onGpsSaved(gpsSavedEvent(null));

        then(notificationSettingReader).should(never()).findBatterySetting(anyString());
        then(batteryAlertStateManager).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("배터리 알림이 꺼져 있으면 억제 상태를 조회하지도 갱신하지도 않는다")
    void onGpsSaved_skips_redis_when_disabled() {
        given(notificationSettingReader.findBatterySetting(LocationFixture.WARD_KEY))
                .willReturn(new BatteryNotificationSetting(false, BatteryThresholds.ofPercents(THRESHOLD_PERCENTS)));

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(15));

        then(batteryAlertStateManager).shouldHaveNoInteractions();
        then(batteryThresholdEvaluator).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("고른 값이 없으면 남은 억제 상태만 지우고 판정하지 않는다")
    void onGpsSaved_deletes_state_when_no_threshold_selected() {
        given(notificationSettingReader.findBatterySetting(LocationFixture.WARD_KEY))
                .willReturn(new BatteryNotificationSetting(true, BatteryThresholds.NONE));

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(15));

        then(batteryAlertStateManager).should(times(1)).delete(LocationFixture.WARD_KEY);
        then(batteryAlertStateManager).should(never()).find(anyString());
        then(batteryThresholdEvaluator).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알릴 것이 없어도 판정 결과 상태는 저장한다")
    void onGpsSaved_saves_state_without_publishing_when_silent() {
        givenEnabledSetting();
        given(batteryAlertStateManager.find(LocationFixture.WARD_KEY))
                .willReturn(new BatteryAlertState(50));
        given(batteryThresholdEvaluator.evaluate(44, THRESHOLD_PERCENTS, 50))
                .willReturn(BatteryEvaluation.silent(new BatteryAlertState(50)));

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(44));

        then(batteryAlertStateManager).should(times(1))
                .save(LocationFixture.WARD_KEY, new BatteryAlertState(50));
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("고른 값에 도달하면 상태를 저장하고 알림 이벤트를 발행한다")
    void onGpsSaved_publishes_event_when_threshold_reached() {
        givenEnabledSetting();
        given(batteryAlertStateManager.find(LocationFixture.WARD_KEY))
                .willReturn(BatteryAlertState.empty());
        given(batteryThresholdEvaluator.evaluate(18, THRESHOLD_PERCENTS, null))
                .willReturn(BatteryEvaluation.alert(20));

        batteryDetectionListener.onGpsSaved(gpsSavedEvent(18));

        then(batteryAlertStateManager).should(times(1))
                .save(LocationFixture.WARD_KEY, new BatteryAlertState(20));
        then(eventPublisher).should(times(1)).publishEvent(eventCaptor.capture());
        BatteryThresholdAlertEvent published = eventCaptor.getValue();
        assertThat(published.memberKey()).isEqualTo(LocationFixture.WARD_KEY);
        assertThat(published.thresholdPercent()).isEqualTo(20);
        assertThat(published.detectedAt()).isEqualTo(LocationFixture.MEASURED_AT);
    }

    private void givenEnabledSetting() {
        given(notificationSettingReader.findBatterySetting(LocationFixture.WARD_KEY))
                .willReturn(new BatteryNotificationSetting(true, BatteryThresholds.ofPercents(THRESHOLD_PERCENTS)));
    }

    private GpsSavedEvent gpsSavedEvent(Integer battery) {
        return new GpsSavedEvent(LocationFixture.WARD_KEY, LocationFixture.createGps(battery));
    }
}
