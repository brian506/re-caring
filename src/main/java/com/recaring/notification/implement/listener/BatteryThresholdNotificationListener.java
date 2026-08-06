package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.location.event.BatteryThresholdAlertEvent;
import com.recaring.notification.implement.NotificationSendManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatteryThresholdNotificationListener {

    private static final String EVENT_TYPE_BATTERY_THRESHOLD = "DEVICE_BATTERY_THRESHOLD";
    private static final String TITLE = "배터리 잔량 알림";

    private final CareRelationshipReader careRelationshipReader;
    private final NotificationSendManager notificationSendManager;

    @Async("broadcastExecutor")
    @EventListener
    public void onBatteryThresholdAlert(BatteryThresholdAlertEvent event) {
        String memberKey = event.memberKey();
        int thresholdPercent = event.thresholdPercent();
        log.info("[배터리 잔량 알림 : 이벤트 수신]: memberKey={} | threshold={}", memberKey, thresholdPercent);

        List<CaregiverInfo> caregivers = careRelationshipReader.findCaregiverInfos(memberKey);
        if (caregivers.isEmpty()) {
            log.warn("[배터리 잔량 알림 : 수신자 없음]: memberKey={}", memberKey);
            return;
        }
        // 보호 대상자와 연관된 보호자 + 관계자 조회
        List<String> guardianKeys = caregivers.stream()
                .filter(caregiver -> caregiver.careRole() == CareRole.GUARDIAN)
                .map(CaregiverInfo::memberKey)
                .toList();
        List<String> managerKeys = caregivers.stream()
                .filter(caregiver -> caregiver.careRole() == CareRole.MANAGER)
                .map(CaregiverInfo::memberKey)
                .toList();

        notificationSendManager.sendToCareParties(
                guardianKeys,
                managerKeys,
                EVENT_TYPE_BATTERY_THRESHOLD,
                TITLE,
                "기기 배터리 잔량이 " + thresholdPercent + "%에 도달했어요.",
                Map.of(
                        "type", EVENT_TYPE_BATTERY_THRESHOLD,
                        "wardKey", memberKey,
                        "thresholdPercent", String.valueOf(thresholdPercent)
                )
        );
    }
}
