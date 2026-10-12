package com.recaring.safezone.controller.request;

import com.recaring.safezone.vo.SafeZoneRadius;
import com.recaring.safezone.dataaccess.entity.SafeZoneType;
import com.recaring.safezone.vo.SafeZoneCreation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSafeZoneRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull Integer radiusMeters,
        @NotNull SafeZoneType type
) {
    public SafeZoneCreation toCommand(String wardMemberKey) {
        return new SafeZoneCreation(wardMemberKey, name, address, latitude, longitude, new SafeZoneRadius(radiusMeters), type);
    }
}
