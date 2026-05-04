package com.recaring.location.implement;

import com.recaring.location.dataaccess.entity.GpsHistory;
import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GpsHistoryWriter {

    private final GpsHistoryRepository gpsHistoryRepository;

    public void save(String wardMemberKey, double latitude, double longitude) {
        gpsHistoryRepository.save(GpsHistory.builder()
                .wardMemberKey(wardMemberKey)
                .latitude(latitude)
                .longitude(longitude)
                .build());
    }
}
