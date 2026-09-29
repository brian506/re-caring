package com.recaring.location.implement.signal;

import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.location.event.LocationSignalLostEvent;
import com.recaring.location.implement.gps.GpsLatestCacheManager;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationSignalMonitor {

    private static final long CHECK_INTERVAL_MILLIS = 10 * 60 * 1000L;
    private static final Duration LOSS_THRESHOLD = Duration.ofMinutes(20);
    private static final Duration TRACKING_WINDOW = Duration.ofHours(24);

    private final CareRelationshipReader careRelationshipReader;
    private final GpsLatestCacheManager gpsLatestCacheManager;
    private final LocationSignalAlertManager locationSignalAlertManager;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = CHECK_INTERVAL_MILLIS, initialDelay = CHECK_INTERVAL_MILLIS)
    public void detectLoss() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lossThreshold = now.minus(LOSS_THRESHOLD);
        LocalDateTime trackingStart = now.minus(TRACKING_WINDOW);

        Map<String, Gps> latestByWard = gpsLatestCacheManager.findAll(careRelationshipReader.findAllWardMemberKeys());
        latestByWard.forEach((wardMemberKey, gps) -> {
            LocalDateTime lastReceivedAt = gps.recordedAt();
            if (lastReceivedAt.isAfter(lossThreshold) || lastReceivedAt.isBefore(trackingStart)) {
                return;
            }
            alert(wardMemberKey, gps);
        });
    }

    private void alert(String wardMemberKey, Gps gps) {
        if (!locationSignalAlertManager.claim(wardMemberKey, gps.recordedAt())) {
            return;
        }

        log.info("[위치 수집 중단 : 감지]: wardMemberKey={} | lastReceivedAt={}", wardMemberKey, gps.recordedAt());
        try {
            eventPublisher.publishEvent(new LocationSignalLostEvent(wardMemberKey, gps));
        } catch (RuntimeException e) {
            locationSignalAlertManager.release(wardMemberKey, gps.recordedAt());
            log.error("[위치 수집 중단 : 알림 처리 실패]: wardMemberKey={} | error={}", wardMemberKey, e.getMessage());
        }
    }
}
