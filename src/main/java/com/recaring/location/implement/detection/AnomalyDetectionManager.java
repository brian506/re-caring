package com.recaring.location.implement.detection;

import com.recaring.location.dataaccess.repository.AnomalyDetectionRepository;
import com.recaring.location.event.AnomalyDetectedEvent;
import com.recaring.location.vo.AnomalyAlert;
import com.recaring.location.vo.AnomalyEvent;
import com.recaring.location.vo.AnomalyEventPeriod;
import com.recaring.location.vo.AnomalyMeasures;
import com.recaring.location.vo.DetectionType;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyDetectionManager {

    private final AnomalyDetectionRepository anomalyDetectionRepository;
    private final AnomalyAcknowledgementManager anomalyAcknowledgementManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void record(AnomalyAlert alert) {
        AnomalyEvent event = alert.event();
        AnomalyMeasures measures = alert.measures();
        int inserted = anomalyDetectionRepository.insertIfAbsent(
                alert.wardMemberKey(),
                alert.detectionType().name(),
                alert.score(),
                alert.recordedAt(),
                alert.latitude(),
                alert.longitude(),
                alert.evidence(),
                event == null ? null : event.alertKind().name(),
                event == null ? null : event.eventId(),
                event == null ? null : event.startedAt(),
                event == null ? null : event.endedAt(),
                event == null ? null : event.categories(),
                alert.reasonCode() == null ? null : alert.reasonCode().name(),
                measures.durationMin(),
                measures.usualMin(),
                measures.speedKmh(),
                measures.hour(),
                event == null ? null : event.remindSeq()
        );
        if (inserted == 0) {
            log.warn("[이상탐지 결과 : 중복 저장 생략]: wardMemberKey={} | detectionType={} | recordedAt={} | alertKind={} | eventId={}",
                    alert.wardMemberKey(), alert.detectionType(), alert.recordedAt(),
                    event == null ? null : event.alertKind(), event == null ? null : event.eventId());
            return;
        }

        refreshAcknowledgement(alert);
        if (alert.isPushed()) {
            eventPublisher.publishEvent(new AnomalyDetectedEvent(alert));
        }
    }

    private void refreshAcknowledgement(AnomalyAlert alert) {
        if (alert.event() == null) {
            return;
        }
        if (alert.isEventEnd()) {
            anomalyAcknowledgementManager.delete(alert.wardMemberKey());
            return;
        }
        // 푸시하지 않은 재알림으로 보호자 확인이 풀리면 안 된다.
        if (alert.isPushed()) {
            anomalyAcknowledgementManager.reset(alert.wardMemberKey(), alert.event().eventId());
        }
    }

    public Optional<Long> findDetectionId(String wardMemberKey, DetectionType detectionType, LocalDateTime recordedAt) {
        return anomalyDetectionRepository.findIdByDetectionKeys(wardMemberKey, detectionType, recordedAt);
    }

    public AnomalyEventPeriod findEventPeriod(String wardMemberKey, String eventId) {
        return anomalyDetectionRepository.findEventPeriod(wardMemberKey, eventId)
                .filter(period -> period.startedAt() != null)
                .orElseThrow(() -> new AppException(ErrorType.ANOMALY_EVENT_NOT_FOUND));
    }

    public void deleteByWardMemberKey(String wardMemberKey) {
        anomalyDetectionRepository.deleteByWardMemberKey(wardMemberKey);
    }
}
