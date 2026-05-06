package com.recaring.location.controller.response;

import com.recaring.location.business.WardLocationCollectionIntervalInfo;

public record WardLocationCollectionIntervalResponse(
        int currentIntervalSeconds
) {
    public static WardLocationCollectionIntervalResponse from(WardLocationCollectionIntervalInfo info) {
        return new WardLocationCollectionIntervalResponse(info.currentIntervalSeconds());
    }
}
