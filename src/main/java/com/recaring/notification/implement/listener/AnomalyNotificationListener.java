package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.location.event.AnomalyDetectedEvent;
import com.recaring.location.vo.AnomalyAlert;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.implement.NotificationSendManager;
import com.recaring.notification.implement.setting.NotificationSettingReader;
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
public class AnomalyNotificationListener {

    private static final DateTimeFormatter DETECTED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String NAME_PLACEHOLDER = "{name}";

    private static final int MAX_BODY_LENGTH = 1000;

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

        // 토글 off이면 알림 스킵
        if (!notificationSettingReader.isAnomalyEnabled(wardMemberKey, detection.detectionType())) {
            return;
        }

        List<CaregiverInfo> caregivers = careRelationshipReader.findCaregiverInfos(wardMemberKey);
        if (caregivers.isEmpty()) {
            log.warn("[이상탐지 알림 : 수신자 없음]: wardMemberKey={}", wardMemberKey);
            return;
        }

        String wardName = memberReader.findNameByMemberKey(wardMemberKey);
        String eventType = detection.detectionType().name();
        notificationSendManager.sendToCareParties(
                memberKeysOf(caregivers, CareRole.GUARDIAN),
                memberKeysOf(caregivers, CareRole.MANAGER),
                eventType,
                detection.detectionType().notificationTitle(),
                buildBody(detection.evidence(), wardName),
                Map.of(
                        "type", eventType,
                        "wardKey", wardMemberKey,
                        "score", String.valueOf(detection.score()),
                        "detectedAt", DETECTED_AT_FORMAT.format(detection.detectedAt()),
                        "latitude", String.valueOf(detection.latitude()),
                        "longitude", String.valueOf(detection.longitude())
                )
        );
        log.info("[이상탐지 알림 : 발송 완료]: wardMemberKey={} | detectionType={} | score={}",
                wardMemberKey, detection.detectionType(), detection.score());
    }

    private String buildBody(String evidence, String wardName) {
        if (evidence == null || evidence.isBlank()) {
            log.warn("[이상탐지 알림 : 근거 문구 없음]: wardName={}", wardName);
            return wardName + "님에게 이상 징후가 감지되었어요.";
        }
        String body = evidence.replace(NAME_PLACEHOLDER, wardName);
        if (body.length() <= MAX_BODY_LENGTH) {
            return body;
        }
        log.warn("[이상탐지 알림 : 본문 길이 초과]: length={}", body.length());
        return body.substring(0, MAX_BODY_LENGTH);
    }

    private List<String> memberKeysOf(List<CaregiverInfo> caregivers, CareRole careRole) {
        return caregivers.stream()
                .filter(caregiver -> caregiver.careRole() == careRole)
                .map(CaregiverInfo::memberKey)
                .toList();
    }
}
