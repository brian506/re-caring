package com.recaring.notification.implement.acknowledgement;

import com.recaring.care.implement.CareRelationshipValidator;
import com.recaring.location.implement.detection.AnomalyAcknowledgementManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAcknowledgementManager {

    private final CareRelationshipValidator careRelationshipValidator;
    private final AnomalyAcknowledgementManager anomalyAcknowledgementManager;

    public void acknowledge(String memberKey, String wardKey, String eventId) {
        careRelationshipValidator.validateCaregiver(memberKey, wardKey);

        boolean applied = anomalyAcknowledgementManager.acknowledge(wardKey, eventId);
        log.info("[AnomalyAlert : Acknowledged]: memberKey={} | wardKey={} | eventId={} | applied={}",
                memberKey, wardKey, eventId, applied);
    }
}
