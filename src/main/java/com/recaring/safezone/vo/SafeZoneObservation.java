package com.recaring.safezone.vo;

import com.recaring.location.vo.Gps;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toCollection;

public record SafeZoneObservation(
        Set<String> insideKeys,
        Set<String> clearlyInsideKeys,
        Set<String> notClearlyOutsideKeys
) {
    private static final double MIN_MARGIN_METERS = 30.0;

    public static SafeZoneObservation of(List<SafeZoneInfo> zones, Gps gps) {
        double margin = Math.max(MIN_MARGIN_METERS, gps.accuracy() == null ? 0 : gps.accuracy());
        double latitude = gps.latitude();
        double longitude = gps.longitude();
        return new SafeZoneObservation(
                keysOf(zones, zone -> zone.contains(latitude, longitude)),
                keysOf(zones, zone -> zone.isClearlyInside(latitude, longitude, margin)),
                keysOf(zones, zone -> !zone.isClearlyOutside(latitude, longitude, margin))
        );
    }

    public Set<String> resolve(Set<String> previousKeys) {
        Set<String> resolved = new LinkedHashSet<>(clearlyInsideKeys);
        previousKeys.stream()
                .filter(notClearlyOutsideKeys::contains)
                .forEach(resolved::add);
        return resolved;
    }

    private static Set<String> keysOf(List<SafeZoneInfo> zones, Predicate<SafeZoneInfo> condition) {
        return zones.stream()
                .filter(condition)
                .map(SafeZoneInfo::safeZoneKey)
                .collect(toCollection(LinkedHashSet::new));
    }
}
