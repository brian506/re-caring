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
    private static final String QUERY_TEMPLATE = "{query}";
    private static final int SEARCH_SIZE = 5;

    private final RestClient kakaoLocalRestClient;

    public KakaoPlaceSearchClient(@Qualifier("kakaoLocalRestClient") RestClient kakaoLocalRestClient) {
        this.kakaoLocalRestClient = kakaoLocalRestClient;
    }

    public List<Place> search(PlaceSearchCondition condition) {
        KakaoKeywordSearchResponse response = request(condition);

        if (response == null || response.documents() == null) {
            log.info("[PlaceSearch : EmptyResponse]: queryLength={}", condition.keyword().value().length());
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
                                .queryParam("query", QUERY_TEMPLATE)
                                .queryParam("size", SEARCH_SIZE);
                        if (condition.hasBias()) {
                            uriBuilder.queryParam("x", condition.longitude())
                                    .queryParam("y", condition.latitude())
                                    .queryParam("radius", condition.radiusMeters());
                        }
                        return uriBuilder.build(query);
                    })
                    .retrieve()
                    .body(KakaoKeywordSearchResponse.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("[PlaceSearch : RateLimitExceeded]: queryLength={} | biased={}", query.length(), condition.hasBias());
            throw new AppException(ErrorType.PLACE_SEARCH_RATE_LIMITED);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.error("[PlaceSearch : KeyAuthFailed]: queryLength={} | status={}", query.length(), e.getStatusCode());
            throw new AppException(ErrorType.PLACE_SEARCH_UNAVAILABLE);
        } catch (HttpClientErrorException.BadRequest e) {
            log.warn("[PlaceSearch : BadRequest]: queryLength={} | biased={} | radius={} | body={}",
                    query.length(), condition.hasBias(), condition.radiusMeters(), e.getResponseBodyAsString());
            throw new AppException(ErrorType.PLACE_SEARCH_UNAVAILABLE);
        } catch (RestClientResponseException e) {
            log.warn("[PlaceSearch : ResponseError]: queryLength={} | status={} | body={}",
                    query.length(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppException(ErrorType.PLACE_SEARCH_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            log.warn("[PlaceSearch : ConnectionFailed]: queryLength={}", query.length(), e.getMostSpecificCause());
            throw new AppException(ErrorType.PLACE_SEARCH_UNAVAILABLE);
        }
    }
}
