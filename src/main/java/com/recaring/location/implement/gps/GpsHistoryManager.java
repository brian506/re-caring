package com.recaring.location.implement.gps;

import com.recaring.location.dataaccess.entity.GpsHistory;
import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GpsHistoryManager {

    private static final int LATEST_LOOKBACK = 20;

    private final GpsHistoryRepository gpsHistoryRepository;

    public List<Gps> findDailyGpsHistory(String wardMemberKey, LocalDate date) {
        return keepDisplayable(gpsHistoryRepository.findDailyGpsHistory(wardMemberKey, date)
                .stream()
                .map(Gps::from)
                .toList());
    }

    public List<Gps> findGpsHistoryBetween(String wardMemberKey, LocalDateTime from, LocalDateTime to) {
        return keepDisplayable(gpsHistoryRepository.findGpsHistoryBetween(wardMemberKey, from, to)
                .stream()
                .map(Gps::from)
                .toList());
    }

    public Optional<Gps> findLatestDisplayable(String wardMemberKey) {
        List<Gps> displayable = keepDisplayable(gpsHistoryRepository.findRecent(wardMemberKey, LATEST_LOOKBACK)
                .reversed()
                .stream()
                .map(Gps::from)
                .toList());
        return displayable.isEmpty() ? Optional.empty() : Optional.of(displayable.getLast());
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

    private List<Gps> keepDisplayable(List<Gps> chronological) {
        List<Gps> accurate = chronological.stream()
                .filter(Gps::isAccurate)
                .toList();
        List<Gps> kept = new ArrayList<>();
        Gps anchor = null;
        for (int i = 0; i < accurate.size(); i++) {
            Gps gps = accurate.get(i);
            Gps next = i + 1 < accurate.size() ? accurate.get(i + 1) : null;
            if (anchor != null && next != null && gps.isSpikeBetween(anchor, next)) {
                continue;
            }
            if (gps.isDisplayableAfter(anchor)) {
                kept.add(gps);
                anchor = gps;
            }
        }
        return kept;
    }
}
