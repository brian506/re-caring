package com.recaring.location.business;

import com.recaring.location.event.GpsReceivedEvent;
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
    private final GpsLatestCacheReader gpsLatestCacheReader;
    private final SseEmitterManager sseEmitterManager;
    private final LocationValidator locationValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void receiveGps(String wardMemberKey, double latitude, double longitude) {
        locationValidator.validateWardRole(wardMemberKey);

        Gps gps = new Gps(latitude, longitude, LocalDateTime.now());

        gpsHistoryWriter.save(wardMemberKey, latitude, longitude);
        eventPublisher.publishEvent(new GpsReceivedEvent(wardMemberKey, gps));
    }

    public SseEmitter streamLocation(String caregiverKey, String wardKey) {
        locationValidator.validateCaregiverAccess(caregiverKey, wardKey);

        SseEmitter emitter = sseEmitterManager.connect(wardKey);

        gpsLatestCacheReader.find(wardKey)
                .ifPresent(gps -> sseEmitterManager.sendInitialEvent(emitter, gps));

        return emitter;
    }

    public List<Gps> getHistory(String caregiverKey, String wardKey, LocalDate date) {
        locationValidator.validateCaregiverAccess(caregiverKey, wardKey);
        return gpsHistoryReader.findByWardKeyAndDate(wardKey, date);
    }
}
