package com.recaring.location.implement;

import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GpsHistoryReader {

    private final GpsHistoryRepository gpsHistoryRepository;

    public List<Gps> findDailyGpsHistory(String wardMemberKey, LocalDate date) {
        return gpsHistoryRepository.findDailyGpsHistory(wardMemberKey, date)
                .stream()
                .map(Gps::from)
                .toList();
    }
}
