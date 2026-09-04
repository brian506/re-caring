package com.recaring.place.controller.response;

import com.recaring.place.vo.Place;

public record PlaceResponse(
        String name,
        String address,
        double latitude,
        double longitude
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.name(),
                place.address(),
                place.latitude(),
                place.longitude()
        );
    }
}
