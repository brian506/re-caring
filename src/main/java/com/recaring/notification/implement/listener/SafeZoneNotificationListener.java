package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.location.event.SafeZoneEnteredEvent;
import com.recaring.location.event.SafeZoneExitedEvent;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.implement.NotificationSendManager;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import com.recaring.notification.vo.SafeZoneAlertType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SafeZoneNotificationListener {

    private static final DateTimeFormatter RECORDED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CareRelationshipReader careRelationshipReader;
    private final NotificationSettingReader notificationSettingReader;
    private final NotificationSendManager notificationSendManager;
    private final MemberReader memberReader;

    @Async("broadcastExecutor")
    @EventListener
    public void onSafeZoneEntered(SafeZoneEnteredEvent event) {
        String wardMemberKey = event.wardMemberKey();
        if (!notificationSettingReader.isSafeZoneEntryEnabled(wardMemberKey)) {
            log.info("[안심존 진입 알림 : 설정 꺼짐 스킵]: wardMemberKey={}", wardMemberKey);
            return;
        }
        send(wardMemberKey, event.safeZoneKey(), SafeZoneAlertType.SAFE_ZONE_ENTERED,
                event.safeZoneName() + "에 도착했어요.", event.latitude(), event.longitude(), event.detectedAt());
    }

    @Async("broadcastExecutor")
    @EventListener
    public void onSafeZoneExited(SafeZoneExitedEvent event) {
        String wardMemberKey = event.wardMemberKey();
        if (!notificationSettingReader.isSafeZoneExitEnabled(wardMemberKey)) {
            log.info("[안심존 이탈 알림 : 설정 꺼짐 스킵]: wardMemberKey={}", wardMemberKey);
            return;
        }
        send(wardMemberKey, event.safeZoneKey(), SafeZoneAlertType.SAFE_ZONE_EXITED,
                event.safeZoneName() + "에서 벗어났어요.", event.latitude(), event.longitude(), event.detectedAt());
    }

    // 보호자는 여러 대상자의 알림을 한 화면에서 본다. 누구에 대한 알림인지 본문 첫머리로 구분한다.
    private void send(
            String wardMemberKey,
            String safeZoneKey,
            SafeZoneAlertType alertType,
            String bodySuffix,
            double latitude,
            double longitude,
            LocalDateTime detectedAt
    ) {
        List<CaregiverInfo> caregivers = careRelationshipReader.findCaregiverInfos(wardMemberKey);
        if (caregivers.isEmpty()) {
            log.warn("[안심존 알림 : 수신자 없음]: wardMemberKey={}", wardMemberKey);
            return;
        }

        String body = memberReader.findNameByMemberKey(wardMemberKey) + "님이 " + bodySuffix;

        List<String> guardianKeys = caregivers.stream()
                .filter(caregiver -> caregiver.careRole().isGuardian())
                .map(CaregiverInfo::memberKey)
                .toList();
        List<String> managerKeys = caregivers.stream()
                .filter(caregiver -> caregiver.careRole() == CareRole.MANAGER)
                .map(CaregiverInfo::memberKey)
                .toList();

        String eventType = alertType.name();
        notificationSendManager.sendToCareParties(
                guardianKeys,
                managerKeys,
                eventType,
                alertType.notificationTitle(),
                body,
                Map.of(
                        "type", eventType,
                        "wardKey", wardMemberKey,
                        "safeZoneKey", safeZoneKey,
                        "recordedAt", RECORDED_AT_FORMAT.format(detectedAt),
                        "latitude", String.valueOf(latitude),
                        "longitude", String.valueOf(longitude)
                )
        );
    }
}
