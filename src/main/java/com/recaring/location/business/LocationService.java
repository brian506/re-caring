package com.recaring.location.business;

import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.implement.LocationValidator;
import com.recaring.location.implement.detection.AnomalyDetectionManager;
import com.recaring.location.implement.gps.GpsHistoryManager;
import com.recaring.location.implement.setting.LocationSettingManager;
import com.recaring.location.implement.sse.SseEmitterManager;
import com.recaring.location.vo.AnomalyEventPeriod;
import com.recaring.location.vo.AnomalyEventRoute;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final GpsHistoryManager gpsHistoryManager;
    private final AnomalyDetectionManager anomalyDetectionManager;
    private final SseEmitterManager sseEmitterManager;
    private final LocationValidator locationValidator;
    private final LocationSettingManager locationSettingManager;
    private final ApplicationEventPublisher eventPublisher;

    public void receiveGps(String wardMemberKey, Gps gps) {
        gpsHistoryManager.save(wardMemberKey, gps);

        // 캐시 write + 배터리/안심존 판정 (GpsLatestCacheListener, BatteryDetectionListener, SafeZoneDetectionListener)
        eventPublisher.publishEvent(new GpsSavedEvent(wardMemberKey, gps));
    }

    public SseEmitter streamLocation(String caregiverKey, String wardKey) {
        locationValidator.validateCaregiverAccess(caregiverKey, wardKey);
        return sseEmitterManager.connect(wardKey, locationSettingManager.findCollectionInterval(wardKey, caregiverKey));
    }

    public List<Gps> getHistory(String requesterKey, String wardKey, LocalDate date) {
        locationValidator.validateHistoryViewAccess(requesterKey, wardKey);
        return gpsHistoryManager.findDailyGpsHistory(wardKey, date);
    }

    public AnomalyEventRoute getAnomalyEventRoute(String caregiverKey, String wardKey, String eventId) {
        locationValidator.validateCaregiverAccess(caregiverKey, wardKey);
        AnomalyEventPeriod period = anomalyDetectionManager.findEventPeriod(wardKey, eventId);
        List<Gps> route = gpsHistoryManager.findGpsHistoryBetween(wardKey, period.startedAt(), period.routeEndAt());
        return new AnomalyEventRoute(period, route);
    }
}
