package com.recaring.notification.implement.listener;

import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.location.event.BatteryThresholdAlertEvent;
import com.recaring.member.implement.MemberReader;
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
    private final MemberReader memberReader;

    @Async("broadcastExecutor")
    @EventListener
    public void onBatteryThresholdAlert(BatteryThresholdAlertEvent event) {
        String memberKey = event.memberKey();
        int thresholdPercent = event.thresholdPercent();
        log.info("[BatteryAlert : EventReceived]: memberKey={} | threshold={}", memberKey, thresholdPercent);

        // 판정 이후 관계가 끊긴 보호자에게 보내지 않도록 현재 관계와 교집합을 취한다.
        List<CaregiverInfo> caregivers = careRelationshipReader.findCaregiverInfos(memberKey).stream()
                .filter(caregiver -> event.recipientKeys().contains(caregiver.memberKey()))
                .toList();
        if (caregivers.isEmpty()) {
            return;
        }
        List<String> recipientKeys = caregivers.stream()
                .map(CaregiverInfo::memberKey)
                .toList();

        notificationSendManager.sendToCaregivers(
                recipientKeys,
                EVENT_TYPE_BATTERY_THRESHOLD,
                TITLE,
                memberReader.findNameByMemberKey(memberKey) + "님의 배터리 잔량이 " + thresholdPercent + "%에 도달했어요.",
                Map.of(
                        "type", EVENT_TYPE_BATTERY_THRESHOLD,
                        "wardKey", memberKey,
                        "thresholdPercent", String.valueOf(thresholdPercent)
                )
        );
    }
}
