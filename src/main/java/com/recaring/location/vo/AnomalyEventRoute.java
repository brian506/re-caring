package com.recaring.location.vo;

import java.util.List;

public record AnomalyEventRoute(
        AnomalyEventPeriod period,
        List<Gps> route
) {
}
