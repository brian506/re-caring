package com.recaring.location.implement.detection;

import com.recaring.location.dataaccess.repository.AnomalyDetectionRepository;
import com.recaring.location.event.AnomalyDetectedEvent;
import com.recaring.location.vo.AnomalyAlert;
import com.recaring.location.vo.AnomalyEvent;
import com.recaring.location.vo.AnomalyMeasures;
import com.recaring.location.vo.DetectionType;
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
        // 중복 수신 시 skip
        if (inserted == 0) {
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
        anomalyAcknowledgementManager.reset(alert.wardMemberKey(), alert.event().eventId());
    }

    public Optional<Long> findDetectionId(String wardMemberKey, DetectionType detectionType, LocalDateTime recordedAt) {
        return anomalyDetectionRepository.findIdByDetectionKeys(wardMemberKey, detectionType, recordedAt);
    }

    public void deleteByWardMemberKey(String wardMemberKey) {
        anomalyDetectionRepository.deleteByWardMemberKey(wardMemberKey);
    }
}
