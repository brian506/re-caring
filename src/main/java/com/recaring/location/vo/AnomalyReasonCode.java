package com.recaring.location.vo;

import java.util.Arrays;
import java.util.Optional;

public enum AnomalyReasonCode {

    VEHICLE,
    DWELL_UNKNOWN,
    DWELL_AFTER_VEHICLE,
    DWELL_KNOWN_LONG,
    DWELL_SAFEZONE_LONG,
    OFF_ROUTE,
    OUTSIDE_SAFEZONE,
    UNFAMILIAR_AREA,
    NIGHT_OUTING,
    RARE_HOUR,
    UNUSUAL_HOUR_AT_PLACE,
    NO_ARRIVAL;

    public static Optional<AnomalyReasonCode> find(String value) {
        return Arrays.stream(values())
                .filter(code -> code.name().equals(value))
                .findFirst();
    }
}
