package com.recaring.location.controller.response;

import com.recaring.location.business.LocationCollectionIntervalSettingInfo;

import java.util.List;

public record LocationCollectionIntervalSettingResponse(
        int currentIntervalSeconds,
        int defaultIntervalSeconds,
        List<Integer> options
) {
    public static LocationCollectionIntervalSettingResponse from(LocationCollectionIntervalSettingInfo info) {
        return new LocationCollectionIntervalSettingResponse(
                info.currentIntervalSeconds(),
                info.defaultIntervalSeconds(),
                info.options()
        );
    }
}
