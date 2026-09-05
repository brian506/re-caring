package com.recaring.place.business;

import com.recaring.place.fixture.PlaceFixture;
import com.recaring.place.implement.KakaoPlaceSearchClient;
import com.recaring.place.vo.Place;
import com.recaring.place.vo.PlaceSearchCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceService 단위 테스트")
class PlaceServiceTest {

    @InjectMocks
    private PlaceService placeService;

    @Mock
    private KakaoPlaceSearchClient kakaoPlaceSearchClient;

    @Test
    @DisplayName("편향 검색이 검색어에 맞는 결과를 3건 주면 전국으로 다시 검색하지 않는다")
    void keeps_biased_result_when_it_has_three_matching_places() {
        PlaceSearchCondition biased = PlaceFixture.createBiasedCondition(PlaceFixture.NEARBY_KEYWORD);
        given(kakaoPlaceSearchClient.search(biased)).willReturn(List.of(
                PlaceFixture.MANGWON_STATION,
                PlaceFixture.OLIVE_YOUNG_MANGWON,
                PlaceFixture.MANGWON_STATION_EXIT));

        List<Place> result = placeService.searchPlaces(biased);

        assertThat(result).containsExactly(
                PlaceFixture.MANGWON_STATION,
                PlaceFixture.OLIVE_YOUNG_MANGWON,
                PlaceFixture.MANGWON_STATION_EXIT);
        then(kakaoPlaceSearchClient).should(never()).search(biased.withoutBias());
    }

    @Test
    @DisplayName("편향 검색 결과가 2건이면 검색어에 맞더라도 전국으로 다시 검색해 전국 결과를 앞에 둔다")
    void falls_back_to_nationwide_when_biased_result_has_two_places() {
        PlaceSearchCondition biased = PlaceFixture.createBiasedCondition(PlaceFixture.NEARBY_KEYWORD);
        given(kakaoPlaceSearchClient.search(biased))
                .willReturn(List.of(PlaceFixture.MANGWON_STATION, PlaceFixture.OLIVE_YOUNG_MANGWON));
        given(kakaoPlaceSearchClient.search(biased.withoutBias()))
                .willReturn(List.of(PlaceFixture.MANGWON_STATION_EXIT));

        List<Place> result = placeService.searchPlaces(biased);

        assertThat(result).containsExactly(
                PlaceFixture.MANGWON_STATION_EXIT,
                PlaceFixture.MANGWON_STATION,
                PlaceFixture.OLIVE_YOUNG_MANGWON);
    }

    @Test
    @DisplayName("편향 검색 결과가 3건이어도 검색어에 맞는 장소가 하나도 없으면 전국으로 다시 검색한다")
    void falls_back_to_nationwide_when_no_biased_place_matches_keyword() {
        PlaceSearchCondition biased = PlaceFixture.createBiasedCondition(PlaceFixture.FAR_KEYWORD);
        given(kakaoPlaceSearchClient.search(biased)).willReturn(List.of(
                PlaceFixture.UNRELATED_RESTAURANT,
                PlaceFixture.UNRELATED_CAFE,
                PlaceFixture.UNRELATED_MART));
        given(kakaoPlaceSearchClient.search(biased.withoutBias()))
                .willReturn(List.of(PlaceFixture.HAEUNDAE_STATION));

        List<Place> result = placeService.searchPlaces(biased);

        assertThat(result).containsExactly(
                PlaceFixture.HAEUNDAE_STATION,
                PlaceFixture.UNRELATED_RESTAURANT,
                PlaceFixture.UNRELATED_CAFE,
                PlaceFixture.UNRELATED_MART);
    }

    @Test
    @DisplayName("전국 결과와 편향 결과에 같은 장소가 있으면 한 번만 남긴다")
    void keeps_one_entry_for_the_same_place_id() {
        PlaceSearchCondition biased = PlaceFixture.createBiasedCondition(PlaceFixture.FAR_KEYWORD);
        given(kakaoPlaceSearchClient.search(biased))
                .willReturn(List.of(PlaceFixture.HAEUNDAE_STATION_JIBUN));
        given(kakaoPlaceSearchClient.search(biased.withoutBias()))
                .willReturn(List.of(PlaceFixture.HAEUNDAE_STATION));

        List<Place> result = placeService.searchPlaces(biased);

        assertThat(result).containsExactly(PlaceFixture.HAEUNDAE_STATION);
    }

    @Test
    @DisplayName("전국 결과와 편향 결과를 합쳐 5건이 넘으면 앞에서 5건만 반환한다")
    void returns_at_most_five_places_after_merge() {
        PlaceSearchCondition biased = PlaceFixture.createBiasedCondition(PlaceFixture.FAR_KEYWORD);
        List<Place> nationwide = List.of(
                PlaceFixture.createPlace("n1", "해운대역 1번출구"),
                PlaceFixture.createPlace("n2", "해운대역 2번출구"),
                PlaceFixture.createPlace("n3", "해운대역 3번출구"),
                PlaceFixture.createPlace("n4", "해운대역 4번출구"),
                PlaceFixture.createPlace("n5", "해운대역 5번출구"));
        given(kakaoPlaceSearchClient.search(biased))
                .willReturn(List.of(PlaceFixture.UNRELATED_RESTAURANT, PlaceFixture.UNRELATED_CAFE));
        given(kakaoPlaceSearchClient.search(biased.withoutBias())).willReturn(nationwide);

        List<Place> result = placeService.searchPlaces(biased);

        assertThat(result).containsExactlyElementsOf(nationwide);
    }

    @Test
    @DisplayName("편향 없이 검색하면 결과가 3건에 못 미쳐도 다시 검색하지 않는다")
    void does_not_search_twice_without_bias() {
        PlaceSearchCondition condition = PlaceFixture.createCondition(PlaceFixture.FAR_KEYWORD);
        given(kakaoPlaceSearchClient.search(condition)).willReturn(List.of(PlaceFixture.HAEUNDAE_STATION));

        List<Place> result = placeService.searchPlaces(condition);

        assertThat(result).containsExactly(PlaceFixture.HAEUNDAE_STATION);
        then(kakaoPlaceSearchClient).should(times(1)).search(condition);
    }

    @Test
    @DisplayName("전국으로 다시 검색해도 결과가 없으면 빈 목록을 반환한다")
    void returns_empty_when_nationwide_search_also_finds_nothing() {
        PlaceSearchCondition biased = PlaceFixture.createBiasedCondition(PlaceFixture.FAR_KEYWORD);
        given(kakaoPlaceSearchClient.search(biased)).willReturn(List.of());
        given(kakaoPlaceSearchClient.search(biased.withoutBias())).willReturn(List.of());

        List<Place> result = placeService.searchPlaces(biased);

        assertThat(result).isEmpty();
    }
}
