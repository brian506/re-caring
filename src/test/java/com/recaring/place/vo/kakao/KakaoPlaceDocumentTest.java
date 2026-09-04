package com.recaring.place.vo.kakao;

import com.recaring.place.vo.Place;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KakaoPlaceDocument 단위 테스트")
class KakaoPlaceDocumentTest {

    private static final String PLACE_NAME = "망원역 6호선";
    private static final String ROAD_ADDRESS = "서울 마포구 월드컵로 지하 77";
    private static final String JIBUN_ADDRESS = "서울 마포구 망원동 484-3";
    private static final String LONGITUDE = "126.910094329982";
    private static final String LATITUDE = "37.5560826563712";

    @Test
    @DisplayName("카카오의 y는 위도로, x는 경도로 옮긴다")
    void converts_kakao_coordinates_to_latitude_and_longitude() {
        KakaoPlaceDocument document =
                new KakaoPlaceDocument(PLACE_NAME, ROAD_ADDRESS, JIBUN_ADDRESS, LONGITUDE, LATITUDE);

        Place place = document.toPlace();

        assertThat(place.latitude()).isEqualTo(37.5560826563712);
        assertThat(place.longitude()).isEqualTo(126.910094329982);
    }

    @Test
    @DisplayName("카카오가 준 장소 이름을 그대로 결과 이름으로 쓴다")
    void uses_kakao_place_name_as_name() {
        KakaoPlaceDocument document =
                new KakaoPlaceDocument(PLACE_NAME, ROAD_ADDRESS, JIBUN_ADDRESS, LONGITUDE, LATITUDE);

        Place place = document.toPlace();

        assertThat(place.name()).isEqualTo(PLACE_NAME);
    }

    @Test
    @DisplayName("도로명 주소가 있는 장소는 도로명 주소를 주소로 쓴다")
    void uses_road_address_when_present() {
        KakaoPlaceDocument document =
                new KakaoPlaceDocument(PLACE_NAME, ROAD_ADDRESS, JIBUN_ADDRESS, LONGITUDE, LATITUDE);

        Place place = document.toPlace();

        assertThat(place.address()).isEqualTo(ROAD_ADDRESS);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("도로명 주소가 없는 장소는 지번 주소를 주소로 쓴다")
    void falls_back_to_jibun_address_without_road_address(String roadAddress) {
        KakaoPlaceDocument document =
                new KakaoPlaceDocument(PLACE_NAME, roadAddress, JIBUN_ADDRESS, LONGITUDE, LATITUDE);

        Place place = document.toPlace();

        assertThat(place.address()).isEqualTo(JIBUN_ADDRESS);
    }

    @ParameterizedTest
    @CsvSource({
            "'', 37.5560826563712",
            "126.910094329982, ''",
            "abc, 37.5560826563712",
            "126.910094329982, abc"
    })
    @DisplayName("좌표가 비어 있거나 숫자가 아닌 장소는 결과에서 제외한다")
    void excludes_place_without_parsable_coordinates(String x, String y) {
        KakaoPlaceDocument document =
                new KakaoPlaceDocument(PLACE_NAME, ROAD_ADDRESS, JIBUN_ADDRESS, x, y);

        assertThat(document.isConvertible()).isFalse();
    }
}
