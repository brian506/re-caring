package com.recaring.location.implement.gps;

import com.recaring.location.dataaccess.entity.GpsHistory;
import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import com.recaring.location.vo.Gps;
import com.recaring.location.vo.GpsReport;
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

    public void save(String wardMemberKey, GpsReport report) {
        gpsHistoryRepository.save(GpsHistory.builder()
                .wardMemberKey(wardMemberKey)
                .latitude(report.latitude())
                .longitude(report.longitude())
                .accuracy(report.accuracy())
                .battery(report.battery())
                .speed(report.speed())
                .measuredAt(report.measuredAt())
                .build());
    }

    public void deleteByWardMemberKey(String wardMemberKey) {
        gpsHistoryRepository.deleteByWardMemberKey(wardMemberKey);
    }
}
