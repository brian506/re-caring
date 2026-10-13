package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.location.event.AnomalyDetectedEvent;
import com.recaring.location.vo.AnomalyAlert;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.implement.NotificationSendManager;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import com.recaring.notification.vo.AnomalyNotificationBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyNotificationListener {

    private static final DateTimeFormatter RECORDED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CareRelationshipReader careRelationshipReader;
    private final NotificationSettingReader notificationSettingReader;
    private final NotificationSendManager notificationSendManager;
    private final MemberReader memberReader;


    // 형제 리스너(SafeZone/Battery/CareInvitation)와 달리 @Async를 쓰지 않는다.
    // 탐지 저장과 알림 발송을 한 트랜잭션으로 묶어 원자적으로 처리하려는 의도다.
    @EventListener
    public void onAnomalyDetected(AnomalyDetectedEvent event) {
        AnomalyAlert detection = event.alert();
        String wardMemberKey = detection.wardMemberKey();

        List<CaregiverInfo> caregivers = notificationSettingReader.filterRecipients(
                wardMemberKey,
                careRelationshipReader.findCaregiverInfos(wardMemberKey),
                settings -> settings.anomaly().isEnabled(detection.detectionType()));
        if (caregivers.isEmpty()) {
            log.info("[이상탐지 알림 : 수신자 없음]: wardMemberKey={} | detectionType={}",
                    wardMemberKey, detection.detectionType());
            return;
        }

        if (detection.reasonCode() == null && (detection.evidence() == null || detection.evidence().isBlank())) {
            log.warn("[이상탐지 알림 : 문구 근거 없음]: wardMemberKey={} | detectionType={}",
                    wardMemberKey, detection.detectionType());
        }

        String wardName = memberReader.findNameByMemberKey(wardMemberKey);
        String eventType = detection.detectionType().name();
        notificationSendManager.sendToCareParties(
                memberKeysOf(caregivers, caregiver -> caregiver.careRole().isGuardian()),
                memberKeysOf(caregivers, caregiver -> caregiver.careRole() == CareRole.MANAGER),
                eventType,
                detection.notificationTitle(),
                AnomalyNotificationBody.of(detection, wardName).value(),
                buildPayload(detection, eventType)
        );
        log.info("[이상탐지 알림 : 발송 완료]: wardMemberKey={} | detectionType={} | reasonCode={} | alertKind={} | score={}",
                wardMemberKey, detection.detectionType(), detection.reasonCode(),
                detection.event() == null ? null : detection.event().alertKind(), detection.score());
    }

    private Map<String, String> buildPayload(AnomalyAlert detection, String eventType) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("type", eventType);
        payload.put("wardKey", detection.wardMemberKey());
        payload.put("score", String.valueOf(detection.score()));
        payload.put("recordedAt", RECORDED_AT_FORMAT.format(detection.recordedAt()));
        payload.put("latitude", String.valueOf(detection.latitude()));
        payload.put("longitude", String.valueOf(detection.longitude()));
        if (detection.event() != null) {
            payload.put("eventId", detection.event().eventId());
            payload.put("alertKind", detection.event().alertKind().name());
        }
        if (detection.reasonCode() != null) {
            payload.put("reasonCode", detection.reasonCode().name());
        }
        return payload;
    }

    private List<String> memberKeysOf(List<CaregiverInfo> caregivers, Predicate<CaregiverInfo> filter) {
        return caregivers.stream()
                .filter(filter)
                .map(CaregiverInfo::memberKey)
                .toList();
    }
}
