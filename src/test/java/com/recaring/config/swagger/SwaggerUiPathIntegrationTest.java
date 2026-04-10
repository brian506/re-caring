package com.recaring.config.swagger;

import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "springdoc.swagger-ui.path=/v3/swagger-ui.html",
        "springdoc.swagger-ui.disable-swagger-default-url=true",
        "springdoc.api-docs.enabled=true",
        "springdoc.api-docs.path=/v3/api-docs"
})
@DisplayName("Swagger UI 경로 설정 통합 테스트")
class SwaggerUiPathIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("springdoc.swagger-ui.path 프로퍼티가 /v3/swagger-ui.html 로 바인딩된다")
    void swaggerUiPath_isConfiguredToV3SwaggerUiHtml() {
        String path = environment.getProperty("springdoc.swagger-ui.path");

        assertThat(path).isEqualTo("/v3/swagger-ui.html");
    }

    @Test
    @DisplayName("springdoc.swagger-ui.path 프로퍼티가 기본값인 /swagger-ui.html 과 다르다")
    void swaggerUiPath_isNotDefaultPath() {
        String path = environment.getProperty("springdoc.swagger-ui.path");

        assertThat(path).isNotEqualTo("/swagger-ui.html");
    }

    @Test
    @DisplayName("springdoc.swagger-ui.disable-swagger-default-url 프로퍼티가 true 로 설정된다")
    void disableSwaggerDefaultUrl_isConfiguredTrue() {
        String disableDefault = environment.getProperty("springdoc.swagger-ui.disable-swagger-default-url");

        assertThat(disableDefault).isEqualToIgnoringCase("true");
    }

    @Test
    @DisplayName("GET /v3/api-docs - 설정된 API 문서 경로에서 200 응답을 반환한다")
    void apiDocsEndpoint_returns200AtConfiguredPath() {
        client.get()
                .uri("/v3/api-docs")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.openapi").isNotEmpty();
    }

    @Test
    @DisplayName("GET /swagger-ui.html - 커스텀 경로 설정 시 기본 경로에서 404를 반환한다")
    void swaggerUiDefaultPath_returns404WhenCustomPathIsConfigured() {
        client.get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /v3/api-docs - API 문서 응답에 OpenAPI info 메타데이터가 포함된다")
    void apiDocsEndpoint_responseContainsApiInfo() {
        client.get()
                .uri("/v3/api-docs")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.info.title").isEqualTo("re;caRing API")
                .jsonPath("$.info.version").isEqualTo("v1");
    }
}