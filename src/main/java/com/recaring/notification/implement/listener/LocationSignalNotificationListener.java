package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.location.event.LocationSignalLostEvent;
import com.recaring.location.vo.Gps;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.implement.NotificationSendManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationSignalNotificationListener {

    private static final String EVENT_TYPE_SIGNAL_LOST = "LOCATION_SIGNAL_LOST";
    private static final String TITLE = "위치 수집 중단 알림";

    private static final DateTimeFormatter RECORDED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CareRelationshipReader careRelationshipReader;
    private final NotificationSendManager notificationSendManager;
    private final MemberReader memberReader;

    @EventListener
    public void onLocationSignalLost(LocationSignalLostEvent event) {
        String wardMemberKey = event.wardMemberKey();
        List<CaregiverInfo> caregivers = careRelationshipReader.findCaregiverInfos(wardMemberKey);
        if (caregivers.isEmpty()) {
            log.warn("[위치 수집 중단 알림 : 수신자 없음]: wardMemberKey={}", wardMemberKey);
            return;
        }

        List<String> guardianKeys = caregivers.stream()
                .filter(caregiver -> caregiver.careRole().isGuardian())
                .map(CaregiverInfo::memberKey)
                .toList();
        List<String> managerKeys = caregivers.stream()
                .filter(caregiver -> caregiver.careRole() == CareRole.MANAGER)
                .map(CaregiverInfo::memberKey)
                .toList();

        notificationSendManager.sendToCareParties(
                guardianKeys,
                managerKeys,
                EVENT_TYPE_SIGNAL_LOST,
                TITLE,
                memberReader.findNameByMemberKey(wardMemberKey)
                        + "님의 위치가 20분 넘게 수집되지 않고 있어요. 기기 전원과 앱 로그인 상태를 확인해 주세요.",
                buildPayload(event)
        );
        log.info("[위치 수집 중단 알림 : 발송 완료]: wardMemberKey={} | lastReceivedAt={}",
                wardMemberKey, event.lastGps().recordedAt());
    }

    private Map<String, String> buildPayload(LocationSignalLostEvent event) {
        Gps lastGps = event.lastGps();
        return Map.of(
                "type", EVENT_TYPE_SIGNAL_LOST,
                "wardKey", event.wardMemberKey(),
                "lastReceivedAt", RECORDED_AT_FORMAT.format(lastGps.recordedAt()),
                "recordedAt", RECORDED_AT_FORMAT.format(lastGps.occurredAt()),
                "latitude", String.valueOf(lastGps.latitude()),
                "longitude", String.valueOf(lastGps.longitude())
        );
    }
}
