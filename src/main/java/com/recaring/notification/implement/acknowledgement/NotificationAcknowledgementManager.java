package com.recaring.notification.implement.acknowledgement;

import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.location.implement.detection.AnomalyAcknowledgementManager;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAcknowledgementManager {

    private final CareRelationshipReader careRelationshipReader;
    private final AnomalyAcknowledgementManager anomalyAcknowledgementManager;

    public void acknowledge(String memberKey, String wardKey, String eventId) {
        if (!careRelationshipReader.exists(wardKey, memberKey)) {
            throw new AppException(ErrorType.NOT_CARE_RELATED_WARD);
        }

        boolean applied = anomalyAcknowledgementManager.acknowledge(wardKey, eventId);
        log.info("[AnomalyAlert : Acknowledged]: memberKey={} | wardKey={} | eventId={} | applied={}",
                memberKey, wardKey, eventId, applied);
    }
}
