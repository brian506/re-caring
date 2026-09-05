package com.recaring.place.vo;

public record Place(
        String placeId,
        String name,
        String address,
        double latitude,
        double longitude
) {
}
