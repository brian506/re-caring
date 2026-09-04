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
    @DisplayName("편향 검색 결과에 검색어가 걸리면 전국으로 다시 검색하지 않는다")
    void keeps_biased_result_when_keyword_matched() {
        PlaceSearchCondition biased = PlaceFixture.createBiasedCondition(PlaceFixture.NEARBY_KEYWORD);
        given(kakaoPlaceSearchClient.search(biased))
                .willReturn(List.of(PlaceFixture.MANGWON_STATION, PlaceFixture.OLIVE_YOUNG_MANGWON));

        List<Place> result = placeService.searchPlaces(biased);

        assertThat(result).containsExactly(PlaceFixture.MANGWON_STATION, PlaceFixture.OLIVE_YOUNG_MANGWON);
        then(kakaoPlaceSearchClient).should(never()).search(biased.withoutBias());
    }

    @Test
    @DisplayName("편향 검색 결과가 비면 전국으로 다시 검색한다")
    void falls_back_to_nationwide_when_biased_result_is_empty() {
        PlaceSearchCondition biased = PlaceFixture.createBiasedCondition(PlaceFixture.FAR_KEYWORD);
        given(kakaoPlaceSearchClient.search(biased)).willReturn(List.of());
        given(kakaoPlaceSearchClient.search(biased.withoutBias()))
                .willReturn(List.of(PlaceFixture.HAEUNDAE_STATION));

        List<Place> result = placeService.searchPlaces(biased);

        assertThat(result).containsExactly(PlaceFixture.HAEUNDAE_STATION);
    }

    @Test
    @DisplayName("편향 반경 안에서 검색어와 무관한 결과만 오면 전국으로 다시 검색한다")
    void falls_back_to_nationwide_when_biased_result_does_not_match_keyword() {
        PlaceSearchCondition biased = PlaceFixture.createBiasedCondition(PlaceFixture.FAR_KEYWORD);
        given(kakaoPlaceSearchClient.search(biased)).willReturn(List.of(PlaceFixture.UNRELATED_RESTAURANT));
        given(kakaoPlaceSearchClient.search(biased.withoutBias()))
                .willReturn(List.of(PlaceFixture.HAEUNDAE_STATION));

        List<Place> result = placeService.searchPlaces(biased);

        assertThat(result).containsExactly(PlaceFixture.HAEUNDAE_STATION);
    }

    @Test
    @DisplayName("편향 없이 검색하면 결과가 비어도 다시 검색하지 않는다")
    void does_not_search_twice_without_bias() {
        PlaceSearchCondition condition = PlaceFixture.createCondition(PlaceFixture.FAR_KEYWORD);
        given(kakaoPlaceSearchClient.search(condition)).willReturn(List.of());

        List<Place> result = placeService.searchPlaces(condition);

        assertThat(result).isEmpty();
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
