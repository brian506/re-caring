package com.recaring.location.business;

import com.recaring.location.vo.LocationCollectionInterval;

import java.util.List;

public record LocationCollectionIntervalSettingInfo(
        int currentIntervalSeconds,
        int defaultIntervalSeconds,
        List<Integer> options
) {
    public static LocationCollectionIntervalSettingInfo from(LocationCollectionInterval currentInterval) {
        return new LocationCollectionIntervalSettingInfo(
                currentInterval.seconds(),
                LocationCollectionInterval.DEFAULT.seconds(),
                LocationCollectionInterval.options()
        );
    }
}
