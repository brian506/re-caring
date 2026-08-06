package com.recaring.location.implement.detection;

import com.recaring.location.event.BatteryThresholdAlertEvent;
import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.vo.BatteryAlertState;
import com.recaring.location.vo.BatteryEvaluation;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import com.recaring.notification.vo.BatteryNotificationSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatteryDetectionListener {

    private final NotificationSettingReader notificationSettingReader;
    private final BatteryAlertStateManager batteryAlertStateManager;
    private final BatteryThresholdEvaluator batteryThresholdEvaluator;
    private final ApplicationEventPublisher eventPublisher;

    @Async("broadcastExecutor")
    @EventListener
    public void onGpsSaved(GpsSavedEvent event) {
        Integer batteryPercent = event.gps().battery();
        if (batteryPercent == null) {
            return;
        }

        String memberKey = event.memberKey();
        BatteryNotificationSetting setting = notificationSettingReader.findBatterySetting(memberKey);
        // 알림이 꺼져 있으면 판정하지 않는다. Redis 상태는 읽지도 쓰지도 않는다.
        if (!setting.enabled()) {
            return;
        }

        // 고른 잔량이 없으면 알릴 것이 없다. 남은 억제 상태만 지워 다음 선택이 옛 값에 막히지 않게 한다.
        if (setting.hasNoThreshold()) {
            // 배터리 알림 여부 상태 캐싱
            batteryAlertStateManager.delete(memberKey);
            return;
        }

        BatteryAlertState currentState = batteryAlertStateManager.find(memberKey);
        // 배터리 알림 판정
        BatteryEvaluation evaluation = batteryThresholdEvaluator.evaluate(
                batteryPercent, setting.thresholds().percents(), currentState.lastNotifiedThreshold());

        batteryAlertStateManager.save(memberKey, evaluation.newState());

        if (!evaluation.shouldNotify()) {
            return;
        }

        log.info("[배터리 잔량 : 선택값 도달]: memberKey={} | battery={} | threshold={}",
                memberKey, batteryPercent, evaluation.reachedThreshold());

        // FCM 알림 보내는 이벤트 호출
        eventPublisher.publishEvent(new BatteryThresholdAlertEvent(
                memberKey, evaluation.reachedThreshold(), event.gps().recordedAt()));
    }
}
