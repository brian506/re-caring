package com.recaring.location.business.command;

import com.recaring.location.vo.LocationCollectionInterval;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record UpdateLocationCollectionIntervalCommand(
        String wardKey,
        LocationCollectionInterval interval
) {
    public UpdateLocationCollectionIntervalCommand {
        if (wardKey == null || wardKey.isBlank()) {
            throw new AppException(ErrorType.INVALID_MEMBER_KEY);
        }
        if (interval == null) {
            throw new AppException(ErrorType.INVALID_LOCATION_COLLECTION_INTERVAL);
        }
    }
}
