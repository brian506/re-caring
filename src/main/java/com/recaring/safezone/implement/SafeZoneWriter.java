package com.recaring.safezone.implement;

import com.recaring.safezone.vo.SafeZoneCreation;
import com.recaring.safezone.vo.SafeZoneUpdate;
import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.repository.SafeZoneRepository;
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

    public void update(SafeZone zone, SafeZoneUpdate command) {
        zone.update(command.name(), command.address(), command.latitude(), command.longitude(), command.radius());
        safeZoneRepository.save(zone);
    }

    public void delete(SafeZone zone) {
        safeZoneRepository.delete(zone);
    }

    public void deleteByWardMemberKey(String wardMemberKey) {
        safeZoneRepository.deleteByWardMemberKey(wardMemberKey);
    }
}
