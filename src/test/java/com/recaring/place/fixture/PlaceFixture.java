package com.recaring.place.fixture;

import com.recaring.place.vo.Place;
import com.recaring.place.vo.PlaceSearchCondition;

public class PlaceFixture {

    public static final String NEARBY_KEYWORD = "망원역";
    public static final String FAR_KEYWORD = "해운대역";

    public static final double SEOUL_LATITUDE = 37.55;
    public static final double SEOUL_LONGITUDE = 126.91;

    public static final Place MANGWON_STATION =
            new Place("망원역 6호선", "서울 마포구 월드컵로 지하 77", 37.5560826, 126.9100943);
    public static final Place OLIVE_YOUNG_MANGWON =
            new Place("올리브영 망원역점", "서울 마포구 월드컵로 75", 37.5556943, 126.9101434);
    public static final Place HAEUNDAE_STATION =
            new Place("해운대역 부산2호선", "부산 해운대구 해운대로 지하 626", 35.1636479, 129.1588972);
    public static final Place UNRELATED_RESTAURANT =
            new Place("해운대연탄생갈비 부평갈산역점", "인천 부평구 주부토로249번길 17", 37.5170444, 126.7264421);

    public static PlaceSearchCondition createBiasedCondition(String query) {
        return PlaceSearchCondition.of(query, SEOUL_LATITUDE, SEOUL_LONGITUDE, null);
    }

    public static PlaceSearchCondition createCondition(String query) {
        return PlaceSearchCondition.of(query, null, null, null);
    }
}
