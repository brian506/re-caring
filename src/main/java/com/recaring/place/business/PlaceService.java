package com.recaring.place.business;

import com.recaring.place.implement.KakaoPlaceSearchClient;
import com.recaring.place.vo.Place;
import com.recaring.place.vo.PlaceSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final KakaoPlaceSearchClient kakaoPlaceSearchClient;

    public List<Place> searchPlaces(PlaceSearchCondition condition) {
        List<Place> places = kakaoPlaceSearchClient.search(condition);

        if (condition.hasBias() && !containsKeyword(places, condition)) {
            return kakaoPlaceSearchClient.search(condition.withoutBias());
        }
        return places;
    }

    private boolean containsKeyword(List<Place> places, PlaceSearchCondition condition) {
        return places.stream()
                .anyMatch(place -> condition.keyword().matches(place.name()));
    }
}
