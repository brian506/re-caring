package com.recaring.place.vo.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoKeywordSearchResponse(
        List<KakaoPlaceDocument> documents
) {
}
