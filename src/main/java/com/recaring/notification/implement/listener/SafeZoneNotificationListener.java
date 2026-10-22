package com.recaring.notification.implement.listener;

import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.safezone.event.SafeZoneEnteredEvent;
import com.recaring.safezone.event.SafeZoneExitedEvent;
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
        send(event.wardMemberKey(), event.safeZoneKey(), event.safeZoneName(),
                SafeZoneAlertType.entered(event.zoneType()), event.latitude(), event.longitude(), event.detectedAt());
    }

    @Async("broadcastExecutor")
    @EventListener
    public void onSafeZoneExited(SafeZoneExitedEvent event) {
        send(event.wardMemberKey(), event.safeZoneKey(), event.safeZoneName(),
                SafeZoneAlertType.exited(event.zoneType()), event.latitude(), event.longitude(), event.detectedAt());
    }

    // 보호자는 여러 대상자의 알림을 한 화면에서 본다. 누구에 대한 알림인지 본문 첫머리로 구분한다.
    private void send(
            String wardMemberKey,
            String safeZoneKey,
            String safeZoneName,
            SafeZoneAlertType alertType,
            double latitude,
            double longitude,
            LocalDateTime detectedAt
    ) {
        List<CaregiverInfo> caregivers = notificationSettingReader.filterRecipients(
                wardMemberKey,
                careRelationshipReader.findCaregiverInfos(wardMemberKey),
                alertType::isEnabledIn);
        if (caregivers.isEmpty()) {
            log.info("[SafeZoneAlert : NoRecipients]: wardMemberKey={} | alertType={}", wardMemberKey, alertType);
            return;
        }

        String wardName = memberReader.findNameByMemberKey(wardMemberKey);
        String eventType = alertType.name();
        Map<String, String> payload = Map.of(
                "type", eventType,
                "wardKey", wardMemberKey,
                "safeZoneKey", safeZoneKey,
                "recordedAt", RECORDED_AT_FORMAT.format(detectedAt),
                "latitude", String.valueOf(latitude),
                "longitude", String.valueOf(longitude)
        );
        CaregiverInfo.groupKeysByWardDisplayName(caregivers, wardName).forEach((displayName, recipientKeys) ->
                notificationSendManager.sendToCaregivers(
                        recipientKeys,
                        eventType,
                        alertType.notificationTitle(),
                        displayName + "님이 " + alertType.bodySuffix(safeZoneName),
                        payload
                ));
    }
}
