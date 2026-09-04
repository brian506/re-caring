package com.recaring.place.vo;

public record PlaceSearchCondition(
        PlaceKeyword keyword,
        Double latitude,
        Double longitude,
        int radiusMeters
) {
    private static final int DEFAULT_RADIUS_METERS = 30_000;
    private static final int MIN_RADIUS_METERS = 0;
    private static final int MAX_RADIUS_METERS = 20_000;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MAX_LONGITUDE = 180.0;

    public static PlaceSearchCondition of(String query, Double latitude, Double longitude, Integer radiusMeters) {
        boolean biased = isValidBias(latitude, longitude);
        return new PlaceSearchCondition(
                new PlaceKeyword(query),
                biased ? latitude : null,
                biased ? longitude : null,
                clampRadius(radiusMeters)
        );
    }

    public boolean hasBias() {
        return latitude != null && longitude != null;
    }

    public PlaceSearchCondition withoutBias() {
        return new PlaceSearchCondition(keyword, null, null, radiusMeters);
    }

    private static boolean isValidBias(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return Math.abs(latitude) <= MAX_LATITUDE && Math.abs(longitude) <= MAX_LONGITUDE;
    }

    private static int clampRadius(Integer radiusMeters) {
        int radius = (radiusMeters == null) ? DEFAULT_RADIUS_METERS : radiusMeters;
        return Math.clamp(radius, MIN_RADIUS_METERS, MAX_RADIUS_METERS);
    }
}
