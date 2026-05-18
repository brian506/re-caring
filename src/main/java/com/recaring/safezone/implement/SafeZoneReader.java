package com.recaring.safezone.implement;

import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.repository.SafeZoneRepository;
import com.recaring.safezone.vo.SafeZoneInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SafeZoneReader {

    private final SafeZoneRepository safeZoneRepository;

    public List<SafeZoneInfo> findAllByWardMemberKey(String wardMemberKey) {
        return safeZoneRepository.findAllByWardMemberKey(wardMemberKey)
                .stream()
                .map(SafeZoneInfo::from)
                .toList();
    }

    public SafeZoneInfo findBySafeZoneKey(String safeZoneKey) {
        return SafeZoneInfo.from(getEntity(safeZoneKey));
    }

    public SafeZone getEntity(String safeZoneKey) {
        return safeZoneRepository.findBySafeZoneKey(safeZoneKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_SAFE_ZONE));
    }
}
