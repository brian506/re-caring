package com.recaring.location.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

import java.util.Arrays;
import java.util.List;

public enum LocationCollectionInterval {
    FIVE_SECONDS(5),
    TEN_SECONDS(10),
    THIRTY_SECONDS(30),
    ONE_MINUTE(60),
    THREE_MINUTES(180),
    FIVE_MINUTES(300);

    public static final LocationCollectionInterval DEFAULT = FIVE_SECONDS;

    private final int seconds;

    LocationCollectionInterval(int seconds) {
        this.seconds = seconds;
    }

    public int seconds() {
        return seconds;
    }

    public static LocationCollectionInterval fromSeconds(Integer seconds) {
        if (seconds == null) {
            throw new AppException(ErrorType.INVALID_LOCATION_COLLECTION_INTERVAL);
        }
        return Arrays.stream(values())
                .filter(interval -> interval.seconds == seconds)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorType.INVALID_LOCATION_COLLECTION_INTERVAL));
    }

    public static List<Integer> options() {
        return Arrays.stream(values())
                .map(LocationCollectionInterval::seconds)
                .toList();
    }
}
