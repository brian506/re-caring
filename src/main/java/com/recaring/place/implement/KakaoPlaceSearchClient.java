package com.recaring.place.implement;

import com.recaring.place.vo.Place;
import com.recaring.place.vo.PlaceSearchCondition;
import com.recaring.place.vo.kakao.KakaoKeywordSearchResponse;
import com.recaring.place.vo.kakao.KakaoPlaceDocument;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
@Component
public class KakaoPlaceSearchClient {

    private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";
    private static final int SEARCH_SIZE = 5;

    private final RestClient kakaoLocalRestClient;

    public KakaoPlaceSearchClient(@Qualifier("kakaoLocalRestClient") RestClient kakaoLocalRestClient) {
        this.kakaoLocalRestClient = kakaoLocalRestClient;
    }

    public List<Place> search(PlaceSearchCondition condition) {
        KakaoKeywordSearchResponse response = request(condition);

        if (response == null || response.documents() == null) {
            log.warn("[장소 검색 : 빈 응답]: query={}", condition.keyword().value());
            return List.of();
        }

        return response.documents().stream()
                .filter(KakaoPlaceDocument::isConvertible)
                .map(KakaoPlaceDocument::toPlace)
                .toList();
    }

    private KakaoKeywordSearchResponse request(PlaceSearchCondition condition) {
        String query = condition.keyword().value();
        try {
            return kakaoLocalRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(KEYWORD_SEARCH_PATH)
                                .queryParam("query", query)
                                .queryParam("size", SEARCH_SIZE);
                        if (condition.hasBias()) {
                            uriBuilder.queryParam("x", condition.longitude())
                                    .queryParam("y", condition.latitude())
                                    .queryParam("radius", condition.radiusMeters());
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(KakaoKeywordSearchResponse.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error("[장소 검색 : 호출 한도 초과]: query={} | biased={}", query, condition.hasBias());
            throw new AppException(ErrorType.PLACE_SEARCH_RATE_LIMITED);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.error("[장소 검색 : 키 인증 실패]: query={} | status={}", query, e.getStatusCode());
            throw new AppException(ErrorType.PLACE_SEARCH_UNAVAILABLE);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("[장소 검색 : 잘못된 요청]: query={} | biased={} | radius={} | body={}",
                    query, condition.hasBias(), condition.radiusMeters(), e.getResponseBodyAsString());
            throw new AppException(ErrorType.PLACE_SEARCH_UNAVAILABLE);
        } catch (RestClientResponseException e) {
            log.error("[장소 검색 : 응답 오류]: query={} | status={} | body={}",
                    query, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppException(ErrorType.PLACE_SEARCH_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            log.error("[장소 검색 : 연결 실패]: query={} | error={}", query, e.getMessage());
            throw new AppException(ErrorType.PLACE_SEARCH_UNAVAILABLE);
        }
    }
}
