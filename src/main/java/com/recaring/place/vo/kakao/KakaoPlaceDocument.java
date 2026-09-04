package com.recaring.place.vo.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.recaring.place.vo.Place;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoPlaceDocument(
        @JsonProperty("place_name") String placeName,
        @JsonProperty("road_address_name") String roadAddressName,
        @JsonProperty("address_name") String addressName,
        String x,
        String y
) {
    public boolean isConvertible() {
        return placeName != null && !placeName.isBlank()
                && isNumeric(x) && isNumeric(y);
    }

    public Place toPlace() {
        return new Place(
                placeName,
                address(),
                Double.parseDouble(y),
                Double.parseDouble(x)
        );
    }

    private String address() {
        if (roadAddressName == null || roadAddressName.isBlank()) {
            return addressName;
        }
        return roadAddressName;
    }

    private boolean isNumeric(String coordinate) {
        if (coordinate == null || coordinate.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(coordinate);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
