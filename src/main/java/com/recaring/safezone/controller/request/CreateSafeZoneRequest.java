package com.recaring.safezone.controller.request;

import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSafeZoneRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull SafeZoneRadius radius
) {
    public CreateSafeZoneCommand toCommand(String wardMemberKey) {
        return new CreateSafeZoneCommand(wardMemberKey, name, address, latitude, longitude, radius);
    }
}
