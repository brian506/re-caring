package com.recaring.safezone.controller.request;

import com.recaring.safezone.vo.SafeZoneRadius;
import com.recaring.safezone.vo.SafeZoneType;
import com.recaring.safezone.vo.SafeZoneUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateSafeZoneRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull Integer radiusMeters,
        @NotNull SafeZoneType type
) {
    public SafeZoneUpdate toCommand() {
        return new SafeZoneUpdate(name, address, latitude, longitude, new SafeZoneRadius(radiusMeters), type);
    }
}
