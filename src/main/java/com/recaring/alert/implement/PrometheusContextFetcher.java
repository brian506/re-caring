package com.recaring.alert.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusContextFetcher {

    private final RestClient restClient;

    @Value("${alert.prometheus.base-url}")
    private String prometheusBaseUrl;

    public String query(String promql, String timeRange) {
        log.info("[Prometheus 조회 : 시작]: promql={}", promql);
        try {
            String uri = UriComponentsBuilder
                    .fromUriString(prometheusBaseUrl + "/api/v1/query")
                    .queryParam("query", promql)
                    .build()
                    .toUriString();

            String result = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            log.info("[Prometheus 조회 : 완료]: promql={}", promql);
            return result != null ? result : "No data";
        } catch (Exception e) {
            log.warn("[Prometheus 조회 : 실패]: promql={} | error={}", promql, e.getMessage());
            return "Failed to query Prometheus: " + e.getMessage();
        }
    }
}
