package com.recaring.config.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    private static final String KAKAO_LOCAL_BASE_URL = "https://dapi.kakao.com";
    private static final String KAKAO_AUTHORIZATION_PREFIX = "KakaoAK ";
    private static final Duration KAKAO_CONNECT_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration KAKAO_READ_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    @Primary
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    public RestClient kakaoLocalRestClient(@Value("${kakao.rest-api-key}") String restApiKey) {
        return RestClient.builder()
                .baseUrl(KAKAO_LOCAL_BASE_URL)
                .requestFactory(kakaoLocalRequestFactory())
                .defaultHeader(HttpHeaders.AUTHORIZATION, KAKAO_AUTHORIZATION_PREFIX + restApiKey)
                .build();
    }

    private JdkClientHttpRequestFactory kakaoLocalRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(KAKAO_CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(KAKAO_READ_TIMEOUT);
        return requestFactory;
    }
}
