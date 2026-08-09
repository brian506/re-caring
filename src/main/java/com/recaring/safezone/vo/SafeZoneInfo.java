package com.recaring.safezone.vo;

import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;

/**
 * 안심존은 중심 좌표 + 반경의 원형이다. 어떤 좌표가 이 존 안에 있는지는 존 자신의 성질이므로
 * 중심까지의 대권 거리(haversine)를 재는 판정을 이 객체가 직접 수행한다.
 */
public record SafeZoneInfo(
        String safeZoneKey,
        String name,
        String address,
        double latitude,
        double longitude,
        SafeZoneRadius radius
) {
    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    public static SafeZoneInfo from(SafeZone entity) {
        return new SafeZoneInfo(
                entity.getSafeZoneKey(),
                entity.getName(),
                entity.getAddress(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getRadius()
        );
    }

    public boolean contains(double targetLatitude, double targetLongitude) {
        return distanceMetersTo(targetLatitude, targetLongitude) <= radius.getMeters();
    }

    private double distanceMetersTo(double targetLatitude, double targetLongitude) {
        double centerLatRad = Math.toRadians(latitude);
        double targetLatRad = Math.toRadians(targetLatitude);
        double deltaLat = Math.toRadians(targetLatitude - latitude);
        double deltaLon = Math.toRadians(targetLongitude - longitude);

        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(centerLatRad) * Math.cos(targetLatRad) * Math.pow(Math.sin(deltaLon / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
