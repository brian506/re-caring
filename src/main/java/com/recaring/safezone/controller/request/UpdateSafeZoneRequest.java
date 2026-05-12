package com.recaring.safezone.controller.request;

import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateSafeZoneRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull SafeZoneRadius radius
) {
    public UpdateSafeZoneCommand toCommand() {
        return new UpdateSafeZoneCommand(name, address, latitude, longitude, radius);
    }
}
