package com.recaring.safezone.implement;

import com.recaring.safezone.controller.request.CreateSafeZoneCommand;
import com.recaring.safezone.controller.request.UpdateSafeZoneCommand;
import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.repository.SafeZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SafeZoneWriter {

    private final SafeZoneRepository safeZoneRepository;

    public void register(CreateSafeZoneCommand command) {
        safeZoneRepository.save(SafeZone.builder()
                .wardMemberKey(command.wardMemberKey())
                .name(command.name())
                .address(command.address())
                .latitude(command.latitude())
                .longitude(command.longitude())
                .radius(command.radius())
                .build());
    }

    public void update(SafeZone zone, UpdateSafeZoneCommand command) {
        zone.update(command.name(), command.address(), command.latitude(), command.longitude(), command.radius());
        safeZoneRepository.save(zone);
    }

    public void delete(SafeZone zone) {
        zone.delete();
        safeZoneRepository.save(zone);
    }
}
