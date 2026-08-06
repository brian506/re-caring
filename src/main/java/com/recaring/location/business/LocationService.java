package com.recaring.location.business;

import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.implement.LocationValidator;
import com.recaring.location.implement.gps.GpsHistoryManager;
import com.recaring.location.implement.sse.SseEmitterManager;
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
    private final SseEmitterManager sseEmitterManager;
    private final LocationValidator locationValidator;
    private final ApplicationEventPublisher eventPublisher;

    public void receiveGps(String wardMemberKey, Gps gps) {
        gpsHistoryManager.save(wardMemberKey, gps);

        // 캐시 write + 배터리/안심존 판정 (GpsLatestCacheListener, BatteryDetectionListener, SafeZoneDetectionListener)
        eventPublisher.publishEvent(new GpsSavedEvent(wardMemberKey, gps));
    }

    public SseEmitter streamLocation(String caregiverKey, String wardKey) {
        locationValidator.validateCaregiverAccess(caregiverKey, wardKey);
        return sseEmitterManager.connect(wardKey);
    }

    public List<Gps> getHistory(String requesterKey, String wardKey, LocalDate date) {
        locationValidator.validateHistoryViewAccess(requesterKey, wardKey);
        return gpsHistoryManager.findDailyGpsHistory(wardKey, date);
    }
}
