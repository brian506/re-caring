package com.recaring.place.fixture;

import com.recaring.place.vo.Place;
import com.recaring.place.vo.PlaceSearchCondition;

public class PlaceFixture {

    public static final String NEARBY_KEYWORD = "망원역";
    public static final String FAR_KEYWORD = "해운대역";

    public static final double SEOUL_LATITUDE = 37.55;
    public static final double SEOUL_LONGITUDE = 126.91;

    public static final Place MANGWON_STATION =
            new Place("8133484", "망원역 6호선", "서울 마포구 월드컵로 지하 77", 37.5560826, 126.9100943);
    public static final Place OLIVE_YOUNG_MANGWON =
            new Place("1234567", "올리브영 망원역점", "서울 마포구 월드컵로 75", 37.5556943, 126.9101434);
    public static final Place MANGWON_STATION_EXIT =
            new Place("2345678", "망원역 2번출구", "서울 마포구 월드컵로 79", 37.5562100, 126.9103300);

    public static final Place HAEUNDAE_STATION =
            new Place("9876543", "해운대역 부산2호선", "부산 해운대구 해운대로 지하 626", 35.1636479, 129.1588972);
    // 같은 장소가 지번 주소로 내려온 경우. placeId 는 같으므로 병합 시 한 건이어야 한다.
    public static final Place HAEUNDAE_STATION_JIBUN =
            new Place("9876543", "해운대역 부산2호선", "부산 해운대구 우동 626", 35.1636479, 129.1588972);

    public static final Place UNRELATED_RESTAURANT =
            new Place("3456789", "해운대연탄생갈비 부평갈산역점", "인천 부평구 주부토로249번길 17", 37.5170444, 126.7264421);
    public static final Place UNRELATED_CAFE =
            new Place("4567890", "해운대커피 신촌점", "서울 서대문구 신촌로 83", 37.5559000, 126.9366000);
    public static final Place UNRELATED_MART =
            new Place("5678901", "해운대수산 마포점", "서울 마포구 독막로 100", 37.5470000, 126.9200000);

    public static Place createPlace(String placeId, String name) {
        return new Place(placeId, name, "서울 마포구 월드컵로 " + placeId, 37.55, 126.91);
    }

    public static PlaceSearchCondition createBiasedCondition(String query) {
        return PlaceSearchCondition.of(query, SEOUL_LATITUDE, SEOUL_LONGITUDE, null);
    }

    public static PlaceSearchCondition createCondition(String query) {
        return PlaceSearchCondition.of(query, null, null, null);
    }
}
