package com.recaring.place.business;

import com.recaring.place.implement.KakaoPlaceSearchClient;
import com.recaring.place.vo.Place;
import com.recaring.place.vo.PlaceSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PlaceService {

    /**
     * 카카오 로컬 키워드 검색은 x/y/radius 를 결과를 잘라내는 필터가 아니라 정렬 가중치로 쓴다.
     * 반경 안에 후보가 없어도 0건이 아니라 반경 밖의 엉뚱한 장소가 소수 딸려온다.
     * 편향 결과가 이 수에 못 미치면 그 반경에 답이 없다는 신호로 본다.
     */
    private static final int MIN_BIASED_RESULT_COUNT = 3;
    private static final int MAX_RESULT_COUNT = 5;

    private final KakaoPlaceSearchClient kakaoPlaceSearchClient;

    public List<Place> searchPlaces(PlaceSearchCondition condition) {
        List<Place> biasedPlaces = kakaoPlaceSearchClient.search(condition);

        if (!condition.hasBias() || isTrustworthy(biasedPlaces, condition)) {
            return biasedPlaces;
        }

        // 전국 결과를 앞에 둔다. 믿을 수 없다고 판정한 편향 결과를 위에 올리면 오답이 1순위가 된다.
        List<Place> nationwidePlaces = kakaoPlaceSearchClient.search(condition.withoutBias());
        return merge(nationwidePlaces, biasedPlaces);
    }

    private boolean isTrustworthy(List<Place> places, PlaceSearchCondition condition) {
        return places.size() >= MIN_BIASED_RESULT_COUNT && containsKeyword(places, condition);
    }

    private boolean containsKeyword(List<Place> places, PlaceSearchCondition condition) {
        return places.stream()
                .anyMatch(place -> condition.keyword().matches(place.name()));
    }

    private List<Place> merge(List<Place> primary, List<Place> secondary) {
        Map<String, Place> merged = new LinkedHashMap<>();
        Stream.concat(primary.stream(), secondary.stream())
                .forEach(place -> merged.putIfAbsent(place.placeId(), place));

        return merged.values().stream()
                .limit(MAX_RESULT_COUNT)
                .toList();
    }
}
