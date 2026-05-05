package com.recaring.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Swagger 설정 단위 테스트")
class SwaggerConfigTest {

    private final SwaggerConfig swaggerConfig = new SwaggerConfig();

    @Test
    @DisplayName("BearerAuth 보안 스키마를 제공한다")
    void openAPI_has_bearer_auth_security_scheme() {
        OpenAPI openAPI = swaggerConfig.openAPI();

        assertThat(openAPI.getSecurity()).anySatisfy(requirement ->
                assertThat(requirement).containsKey("BearerAuth"));
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKey("BearerAuth");
        assertThat(openAPI.getComponents().getSecuritySchemes().get("BearerAuth").getScheme())
                .isEqualTo("bearer");
        assertThat(openAPI.getComponents().getSecuritySchemes().get("BearerAuth").getBearerFormat())
                .isEqualTo("JWT");
    }
}
