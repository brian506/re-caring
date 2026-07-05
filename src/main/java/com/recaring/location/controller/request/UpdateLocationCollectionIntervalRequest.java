package com.recaring.location.controller.request;

import com.recaring.location.vo.LocationCollectionInterval;
import jakarta.validation.constraints.NotNull;

public record UpdateLocationCollectionIntervalRequest(
        @NotNull Integer intervalSeconds
) {
    public LocationCollectionInterval toInterval() {
        return LocationCollectionInterval.fromSeconds(intervalSeconds);
    }
}
