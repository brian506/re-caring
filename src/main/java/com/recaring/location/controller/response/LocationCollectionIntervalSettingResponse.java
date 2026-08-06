package com.recaring.location.controller.response;

import com.recaring.location.vo.LocationCollectionInterval;

import java.util.List;

public record LocationCollectionIntervalSettingResponse(
        int currentIntervalSeconds,
        int defaultIntervalSeconds,
        List<Integer> options
) {
    public static LocationCollectionIntervalSettingResponse from(LocationCollectionInterval currentInterval) {
        return new LocationCollectionIntervalSettingResponse(
                currentInterval.seconds(),
                LocationCollectionInterval.DEFAULT.seconds(),
                LocationCollectionInterval.options()
        );
    }
}
