package com.recaring.location.implement.detection;

import com.recaring.location.dataaccess.repository.AnomalyDetectionRepository;
import com.recaring.location.event.AnomalyDetectedEvent;
import com.recaring.location.vo.AnomalyAlert;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void record(AnomalyAlert alert) {
        int inserted = anomalyDetectionRepository.insertIfAbsent(
                alert.wardMemberKey(),
                alert.detectionType().name(),
                alert.score(),
                alert.recordedAt(),
                alert.latitude(),
                alert.longitude(),
                alert.evidence()
        );
        // 중복 수신 시 skip
        if (inserted == 0) {
            return;
        }

        eventPublisher.publishEvent(new AnomalyDetectedEvent(alert));
    }

    public Optional<Long> findDetectionId(String wardMemberKey, DetectionType detectionType, LocalDateTime recordedAt) {
        return anomalyDetectionRepository.findIdByDetectionKeys(wardMemberKey, detectionType, recordedAt);
    }

    public void deleteByWardMemberKey(String wardMemberKey) {
        anomalyDetectionRepository.deleteByWardMemberKey(wardMemberKey);
    }
}
