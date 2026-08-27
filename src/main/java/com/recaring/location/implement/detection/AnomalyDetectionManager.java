package com.recaring.location.implement.detection;

import com.recaring.location.dataaccess.repository.AnomalyDetectionRepository;
import com.recaring.location.event.AnomalyDetectedEvent;
import com.recaring.location.vo.AnomalyAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
                alert.detectedAt(),
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

    public void deleteByWardMemberKey(String wardMemberKey) {
        anomalyDetectionRepository.deleteByWardMemberKey(wardMemberKey);
    }
}
