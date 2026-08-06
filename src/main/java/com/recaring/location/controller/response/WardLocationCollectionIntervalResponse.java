package com.recaring.location.controller.response;

import com.recaring.location.vo.LocationCollectionInterval;

public record WardLocationCollectionIntervalResponse(
        int currentIntervalSeconds
) {
    public static WardLocationCollectionIntervalResponse from(LocationCollectionInterval currentInterval) {
        return new WardLocationCollectionIntervalResponse(currentInterval.seconds());
    }
}
