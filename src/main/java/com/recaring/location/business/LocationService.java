package com.recaring.location.business;

import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.implement.LocationValidator;
import com.recaring.location.implement.gps.GpsHistoryManager;
import com.recaring.location.implement.sse.SseEmitterManager;
import com.recaring.location.vo.Gps;
import com.recaring.location.vo.GpsReport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final GpsHistoryManager gpsHistoryManager;
    private final SseEmitterManager sseEmitterManager;
    private final LocationValidator locationValidator;
    private final ApplicationEventPublisher eventPublisher;

    public void receiveGps(String wardMemberKey, GpsReport report) {
        Gps gps = report.toGps(LocalDateTime.now());

        gpsHistoryManager.save(wardMemberKey, report);

        // 캐시 write + 배터리/안심존 판정 (GpsLatestCacheListener, BatteryDetectionListener, SafeZoneDetectionListener)
        eventPublisher.publishEvent(new GpsSavedEvent(wardMemberKey, gps));
    }

    public SseEmitter streamLocation(String caregiverKey, String wardKey) {
        locationValidator.validateCaregiverAccess(caregiverKey, wardKey);
        return sseEmitterManager.connect(wardKey);
    }

    public List<Gps> getHistory(String caregiverKey, String wardKey, LocalDate date) {
        locationValidator.validateCaregiverAccess(caregiverKey, wardKey);
        return gpsHistoryManager.findDailyGpsHistory(wardKey, date);
    }
}
