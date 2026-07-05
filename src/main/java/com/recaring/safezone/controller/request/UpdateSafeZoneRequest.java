package com.recaring.safezone.controller.request;

import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;
import com.recaring.safezone.vo.SafeZoneUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateSafeZoneRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull SafeZoneRadius radius
) {
    public SafeZoneUpdate toCommand() {
        return new SafeZoneUpdate(name, address, latitude, longitude, radius);
    }
}
