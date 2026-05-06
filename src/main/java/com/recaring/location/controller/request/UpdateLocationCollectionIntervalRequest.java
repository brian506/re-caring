package com.recaring.location.controller.request;

import com.recaring.location.business.command.UpdateLocationCollectionIntervalCommand;
import com.recaring.location.vo.LocationCollectionInterval;
import jakarta.validation.constraints.NotNull;

public record UpdateLocationCollectionIntervalRequest(
        @NotNull Integer intervalSeconds
) {
    public UpdateLocationCollectionIntervalCommand toCommand(String wardKey) {
        return new UpdateLocationCollectionIntervalCommand(
                wardKey,
                LocationCollectionInterval.fromSeconds(intervalSeconds)
        );
    }
}
