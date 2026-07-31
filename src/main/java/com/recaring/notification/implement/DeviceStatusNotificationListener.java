package com.recaring.notification.implement;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.location.event.DeviceStatusAlertEvent;
import com.recaring.location.vo.DeviceState;
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
public class DeviceStatusNotificationListener {

    private static final String EVENT_TYPE_LOW_BATTERY = "DEVICE_LOW_BATTERY";
    private static final String EVENT_TYPE_FULL_BATTERY = "DEVICE_FULL_BATTERY";
    private static final String EVENT_TYPE_OFFLINE = "DEVICE_OFFLINE";

    private final CareRelationshipReader careRelationshipReader;
    private final NotificationSettingReader notificationSettingReader;
    private final NotificationSendManager notificationSendManager;

    @Async("broadcastExecutor")
    @EventListener
    public void onDeviceStatusAlert(DeviceStatusAlertEvent event) {
        String memberKey = event.memberKey();
        log.info("[기기상태 알림 : 이벤트 수신]: memberKey={} | newState={}", memberKey, event.newState());

        boolean isBatteryAlert = event.newState() == DeviceState.LOW_BATTERY
                || event.newState() == DeviceState.FULL_BATTERY;
        if (isBatteryAlert && !notificationSettingReader.isLowBatteryEnabled(memberKey)) {
            log.info("[기기상태 알림 : 설정 꺼짐 스킵]: memberKey={} | newState={}", memberKey, event.newState());
            return;
        }

        List<CaregiverInfo> caregivers = careRelationshipReader.findCaregiverInfos(memberKey);
        if (caregivers.isEmpty()) {
            log.warn("[기기상태 알림 : 수신자 없음]: memberKey={}", memberKey);
            return;
        }

        List<String> guardianKeys = caregivers.stream()
                .filter(caregiver -> caregiver.careRole() == CareRole.GUARDIAN)
                .map(CaregiverInfo::memberKey)
                .toList();
        List<String> managerKeys = caregivers.stream()
                .filter(caregiver -> caregiver.careRole() == CareRole.MANAGER)
                .map(CaregiverInfo::memberKey)
                .toList();

        String eventType;
        String title;
        String body;
        if (event.newState() == DeviceState.LOW_BATTERY) {
            eventType = EVENT_TYPE_LOW_BATTERY;
            title = "배터리 부족 알림";
            body = "기기 배터리가 부족해요. 확인이 필요해요.";
        } else if (event.newState() == DeviceState.FULL_BATTERY) {
            eventType = EVENT_TYPE_FULL_BATTERY;
            title = "배터리 충전 완료";
            body = "기기 배터리가 충분히 충전됐어요.";
        } else if (event.newState() == DeviceState.OFFLINE) {
            eventType = EVENT_TYPE_OFFLINE;
            title = "기기 연결 끊김";
            body = "기기와 연결이 끊겼어요. 확인이 필요해요.";
        } else {
            return;
        }

        notificationSendManager.sendToCareParties(
                guardianKeys,
                managerKeys,
                eventType,
                title,
                body,
                Map.of("type", eventType, "wardKey", memberKey)
        );
    }
}
