package com.recaring.location.implement.gps;

import com.recaring.location.dataaccess.entity.GpsHistory;
import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GpsHistoryManager {

    private final GpsHistoryRepository gpsHistoryRepository;

    public List<Gps> findDailyGpsHistory(String wardMemberKey, LocalDate date) {
        return gpsHistoryRepository.findDailyGpsHistory(wardMemberKey, date)
                .stream()
                .map(Gps::from)
                .toList();
    }

    public void save(String wardMemberKey, Gps gps) {
        gpsHistoryRepository.save(GpsHistory.builder()
                .wardMemberKey(wardMemberKey)
                .latitude(gps.latitude())
                .longitude(gps.longitude())
                .recordedAt(gps.recordedAt())
                .accuracy(gps.accuracy())
                .battery(gps.battery())
                .speed(gps.speed())
                .measuredAt(gps.measuredAt())
                .build());
    }

    public void deleteByWardMemberKey(String wardMemberKey) {
        gpsHistoryRepository.deleteByWardMemberKey(wardMemberKey);
    }
}
