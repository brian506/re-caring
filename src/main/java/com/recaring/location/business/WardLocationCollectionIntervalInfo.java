package com.recaring.location.business;

import com.recaring.location.vo.LocationCollectionInterval;

public record WardLocationCollectionIntervalInfo(
        int currentIntervalSeconds) {
    public static WardLocationCollectionIntervalInfo from(LocationCollectionInterval currentInterval) {
        return new WardLocationCollectionIntervalInfo(currentInterval.seconds());
    }
}
