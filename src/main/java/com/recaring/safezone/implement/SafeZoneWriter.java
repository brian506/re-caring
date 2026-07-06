package com.recaring.safezone.implement;

import com.recaring.safezone.vo.SafeZoneCreation;
import com.recaring.safezone.vo.SafeZoneUpdate;
import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.repository.SafeZoneRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SafeZoneWriter {

    private final SafeZoneRepository safeZoneRepository;

    public void register(SafeZoneCreation command) {
        safeZoneRepository.save(SafeZone.builder()
                .wardMemberKey(command.wardMemberKey())
                .name(command.name())
                .address(command.address())
                .latitude(command.latitude())
                .longitude(command.longitude())
                .radius(command.radius())
                .build());
    }

    public void update(String safeZoneKey, SafeZoneUpdate command) {
        SafeZone zone = getEntity(safeZoneKey);
        zone.update(command.name(), command.address(), command.latitude(), command.longitude(), command.radius());
    }

    public void delete(String safeZoneKey) {
        safeZoneRepository.delete(getEntity(safeZoneKey));
    }

    private SafeZone getEntity(String safeZoneKey) {
        return safeZoneRepository.findBySafeZoneKey(safeZoneKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_SAFE_ZONE));
    }

    public void deleteByWardMemberKey(String wardMemberKey) {
        safeZoneRepository.deleteByWardMemberKey(wardMemberKey);
    }
}
