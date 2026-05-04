package com.recaring.location.implement;

import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GpsHistoryReader {

    private final GpsHistoryRepository gpsHistoryRepository;

    public List<Gps> findByWardKeyAndDate(String wardMemberKey, LocalDate date) {
        return gpsHistoryRepository.findByWardKeyAndDate(wardMemberKey, date)
                .stream()
                .map(Gps::from)
                .toList();
    }

    public List<String> findActiveWardKeysSince(LocalDateTime since) {
        return gpsHistoryRepository.findActiveWardKeysSince(since);
    }

    public List<Gps> findByWardKeyBetween(String wardMemberKey, LocalDateTime from, LocalDateTime to) {
        return gpsHistoryRepository.findByWardKeyBetween(wardMemberKey, from, to)
                .stream()
                .map(Gps::from)
                .toList();
    }
}
