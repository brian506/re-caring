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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

        String wardKey = event.memberKey();
        Map<String, BatteryNotificationSetting> settings = notificationSettingReader.findBatterySettings(wardKey);
        if (settings.isEmpty()) {
            return;
        }

        Map<String, BatteryAlertState> states = batteryAlertStateManager.findAll(wardKey);
        Map<Integer, List<String>> recipientsByThreshold = new TreeMap<>();

        settings.forEach((caregiverKey, setting) -> {
            // 알림이 꺼진 보호자는 판정하지 않는다. 끌 때 상태를 지웠으므로 건드릴 것도 없다.
            if (!setting.enabled()) {
                return;
            }
            // 고른 잔량이 없으면 남은 억제 상태만 지워 다음 선택이 옛 값에 막히지 않게 한다.
            if (setting.hasNoThreshold()) {
                batteryAlertStateManager.delete(wardKey, caregiverKey);
                return;
            }

            BatteryEvaluation evaluation = batteryThresholdEvaluator.evaluate(
                    batteryPercent,
                    setting.thresholds().percents(),
                    states.getOrDefault(caregiverKey, BatteryAlertState.empty()).lastNotifiedThreshold());
            batteryAlertStateManager.save(wardKey, caregiverKey, evaluation.newState());

            if (evaluation.shouldNotify()) {
                recipientsByThreshold
                        .computeIfAbsent(evaluation.reachedThreshold(), threshold -> new ArrayList<>())
                        .add(caregiverKey);
            }
        });

        recipientsByThreshold.forEach((threshold, recipientKeys) -> {
            log.info("[BatteryAlert : ThresholdReached]: wardKey={} | battery={} | threshold={} | recipients={}",
                    wardKey, batteryPercent, threshold, recipientKeys.size());
            eventPublisher.publishEvent(new BatteryThresholdAlertEvent(
                    wardKey, threshold, event.gps().occurredAt(), List.copyOf(recipientKeys)));
        });
    }
}
