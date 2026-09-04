package com.recaring.place.controller;

import com.recaring.care.fixture.CareFixture;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.place.fixture.PlaceFixture;
import com.recaring.place.implement.KakaoPlaceSearchClient;
import com.recaring.place.vo.PlaceSearchCondition;
import com.recaring.support.AbstractIntegrationTest;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@DisplayName("장소 검색 API HTTP 통합 테스트")
class PlaceControllerTest extends AbstractIntegrationTest {

    private static final String SEARCH_URI = "/api/v1/places/search?query={query}";
    private static final String SEARCH_URI_WITH_BIAS =
            "/api/v1/places/search?query={query}&latitude={latitude}&longitude={longitude}&radiusMeters={radiusMeters}";
    private static final String SEARCH_URI_WITHOUT_QUERY = "/api/v1/places/search";
    private static final String BLANK_QUERY = "   ";

    private static final int APP_DEFAULT_RADIUS_METERS = 30_000;
    private static final int KAKAO_MAX_RADIUS_METERS = 20_000;

    @MockitoBean
    private KakaoPlaceSearchClient kakaoPlaceSearchClient;

    private String guardianAuth;
    private PlaceSearchCondition expectedCondition;

    @BeforeEach
    void setUpAuth() {
        guardianAuth = bearerToken(CareFixture.GUARDIAN_MEMBER_KEY, MemberRole.GUARDIAN);
        expectedCondition = PlaceFixture.createCondition(PlaceFixture.NEARBY_KEYWORD);
    }

    @Test
    @DisplayName("GET /api/v1/places/search - 로그인한 보호자는 장소 후보 목록을 검색 순서대로 받는다")
    void searchPlaces_returns_candidates_in_order() {
        given(kakaoPlaceSearchClient.search(expectedCondition))
                .willReturn(List.of(PlaceFixture.MANGWON_STATION, PlaceFixture.OLIVE_YOUNG_MANGWON));

        client.get()
                .uri(SEARCH_URI, PlaceFixture.NEARBY_KEYWORD)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[0].name").isEqualTo(PlaceFixture.MANGWON_STATION.name())
                .jsonPath("$.data[0].address").isEqualTo(PlaceFixture.MANGWON_STATION.address())
                .jsonPath("$.data[0].latitude").isEqualTo(PlaceFixture.MANGWON_STATION.latitude())
                .jsonPath("$.data[0].longitude").isEqualTo(PlaceFixture.MANGWON_STATION.longitude())
                .jsonPath("$.data[1].name").isEqualTo(PlaceFixture.OLIVE_YOUNG_MANGWON.name());
    }

    @Test
    @DisplayName("GET /api/v1/places/search - 좌표와 반경을 함께 보내면 카카오 상한으로 잘린 편향 조건으로 검색한다")
    void searchPlaces_applies_bias_with_clamped_radius() {
        PlaceSearchCondition biasedCondition = PlaceSearchCondition.of(
                PlaceFixture.NEARBY_KEYWORD,
                PlaceFixture.SEOUL_LATITUDE,
                PlaceFixture.SEOUL_LONGITUDE,
                KAKAO_MAX_RADIUS_METERS
        );
        given(kakaoPlaceSearchClient.search(biasedCondition))
                .willReturn(List.of(PlaceFixture.MANGWON_STATION));

        client.get()
                .uri(SEARCH_URI_WITH_BIAS,
                        PlaceFixture.NEARBY_KEYWORD,
                        PlaceFixture.SEOUL_LATITUDE,
                        PlaceFixture.SEOUL_LONGITUDE,
                        APP_DEFAULT_RADIUS_METERS)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].name").isEqualTo(PlaceFixture.MANGWON_STATION.name());
    }

    @Test
    @DisplayName("GET /api/v1/places/search - 검색 결과가 없으면 에러가 아니라 빈 배열을 반환한다")
    void searchPlaces_returns_empty_array_when_no_result() {
        given(kakaoPlaceSearchClient.search(expectedCondition)).willReturn(List.of());

        client.get()
                .uri(SEARCH_URI, PlaceFixture.NEARBY_KEYWORD)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/v1/places/search - 검색어가 공백이면 400을 반환한다")
    void searchPlaces_returns_400_when_query_is_blank() {
        client.get()
                .uri(SEARCH_URI, BLANK_QUERY)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E10000");
    }

    @Test
    @DisplayName("GET /api/v1/places/search - 검색어 파라미터가 없으면 400을 반환한다")
    void searchPlaces_returns_400_when_query_is_missing() {
        client.get()
                .uri(SEARCH_URI_WITHOUT_QUERY)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E10000");
    }

    @Test
    @DisplayName("GET /api/v1/places/search - 인증 없이 호출하면 401을 반환한다")
    void searchPlaces_returns_401_without_authentication() {
        client.get()
                .uri(SEARCH_URI, PlaceFixture.NEARBY_KEYWORD)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/v1/places/search - 카카오 호출이 실패하면 502를 반환한다")
    void searchPlaces_returns_502_when_kakao_call_fails() {
        willThrow(new AppException(ErrorType.PLACE_SEARCH_UNAVAILABLE))
                .given(kakaoPlaceSearchClient).search(expectedCondition);

        client.get()
                .uri(SEARCH_URI, PlaceFixture.NEARBY_KEYWORD)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E10001");
    }

    @Test
    @DisplayName("GET /api/v1/places/search - 카카오 호출 한도를 넘으면 429를 반환한다")
    void searchPlaces_returns_429_when_kakao_quota_exceeded() {
        willThrow(new AppException(ErrorType.PLACE_SEARCH_RATE_LIMITED))
                .given(kakaoPlaceSearchClient).search(expectedCondition);

        client.get()
                .uri(SEARCH_URI, PlaceFixture.NEARBY_KEYWORD)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isEqualTo(429)
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E10002");
    }
}
