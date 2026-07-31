package com.recaring.location.business;

import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.implement.*;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final GpsHistoryWriter gpsHistoryWriter;
    private final GpsHistoryReader gpsHistoryReader;
    private final GpsLatestCacheWriter gpsLatestCacheWriter;
    private final SseEmitterManager sseEmitterManager;
    private final LocationValidator locationValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void receiveGps(String wardMemberKey, double latitude, double longitude, Double accuracy, Integer battery) {
        Gps gps = new Gps(latitude, longitude, LocalDateTime.now(), accuracy, battery);

        gpsLatestCacheWriter.save(wardMemberKey, gps);
        gpsHistoryWriter.save(wardMemberKey, latitude, longitude, accuracy, battery);

        eventPublisher.publishEvent(new GpsSavedEvent(wardMemberKey, gps));
    }

    public SseEmitter streamLocation(String caregiverKey, String wardKey) {
        locationValidator.validateCaregiverAccess(caregiverKey, wardKey);
        return sseEmitterManager.connect(wardKey);
    }

    public List<Gps> getHistory(String caregiverKey, String wardKey, LocalDate date) {
        locationValidator.validateCaregiverAccess(caregiverKey, wardKey);
        return gpsHistoryReader.findDailyGpsHistory(wardKey, date);
    }
}
