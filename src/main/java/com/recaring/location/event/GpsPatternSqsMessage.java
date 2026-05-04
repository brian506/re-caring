package com.recaring.location.event;

import com.recaring.location.vo.Gps;

import java.util.List;

public record GpsPatternSqsMessage(
        String wardMemberKey,
        List<GpsPoint> gpsPoints
) {
    public record GpsPoint(double lat, double lng, String recordedAt) {}

    public static GpsPatternSqsMessage from(String wardMemberKey, List<Gps> gpsList) {
        List<GpsPoint> points = gpsList.stream()
                .map(gps -> new GpsPoint(gps.lat(), gps.lng(), gps.recordedAt().toString()))
                .toList();
        return new GpsPatternSqsMessage(wardMemberKey, points);
    }
}
