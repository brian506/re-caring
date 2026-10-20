package com.recaring.safezone.vo;

import com.recaring.location.vo.Gps;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toCollection;

public record SafeZoneObservation(
        Set<String> insideKeys,
        Set<String> clearlyInsideKeys,
        Set<String> notClearlyOutsideKeys,
        Map<String, String> zoneVersions
) {
    private static final double MIN_MARGIN_METERS = 30.0;

    public static SafeZoneObservation of(List<SafeZoneInfo> zones, Gps gps) {
        double margin = Math.max(MIN_MARGIN_METERS, gps.accuracy() == null ? 0 : gps.accuracy());
        double latitude = gps.latitude();
        double longitude = gps.longitude();
        Map<String, String> zoneVersions = new LinkedHashMap<>();
        zones.forEach(zone -> zoneVersions.put(zone.safeZoneKey(), zone.version()));
        return new SafeZoneObservation(
                keysOf(zones, zone -> zone.contains(latitude, longitude)),
                keysOf(zones, zone -> zone.isClearlyInside(latitude, longitude, margin)),
                keysOf(zones, zone -> !zone.isClearlyOutside(latitude, longitude, margin)),
                zoneVersions
        );
    }

    public Set<String> keysWithSameVersion(Map<String, String> previousVersions) {
        return zoneVersions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(previousVersions.get(entry.getKey())))
                .map(Map.Entry::getKey)
                .collect(toCollection(LinkedHashSet::new));
    }

    // 직전과 버전이 같은 존은 히스테리시스로 판정하고, 새로 생겼거나 바뀐 존은 지금 위치로 기준선만 세운다.
    public Set<String> resolve(Set<String> previousKeys, Set<String> comparableKeys) {
        return zoneVersions.keySet().stream()
                .filter(key -> comparableKeys.contains(key)
                        ? clearlyInsideKeys.contains(key)
                                || (previousKeys.contains(key) && notClearlyOutsideKeys.contains(key))
                        : insideKeys.contains(key))
                .collect(toCollection(LinkedHashSet::new));
    }

    private static Set<String> keysOf(List<SafeZoneInfo> zones, Predicate<SafeZoneInfo> condition) {
        return zones.stream()
                .filter(condition)
                .map(SafeZoneInfo::safeZoneKey)
                .collect(toCollection(LinkedHashSet::new));
    }
}
