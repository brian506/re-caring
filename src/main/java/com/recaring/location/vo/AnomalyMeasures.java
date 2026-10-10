package com.recaring.location.vo;

public record AnomalyMeasures(
        Integer durationMin,
        Integer usualMin,
        Integer speedKmh,
        Integer hour
) {
}
