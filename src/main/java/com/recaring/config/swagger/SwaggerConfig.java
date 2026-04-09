package com.recaring.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("/").description("현재 서버")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Access Token을 입력하세요. 'Bearer ' 접두사 없이 토큰만 입력하면 됩니다.")
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("re;caRing API")
                .description("""
                        re;caRing 서비스 REST API 문서입니다.

                        ## 인증 방식
                        - **Access Token**: JWT, 로그인 응답 바디의 `accessToken` 필드
                        - **Refresh Token**: HttpOnly Cookie (`refreshToken`)로 자동 전송

                        ## 역할(Role)
                        - `GUARDIAN` (보호자): 보호 대상자 추가, 보호 대상자 목록 조회 등 가능
                        - `WARD` (보호 대상자): 케어 요청 수락/거절, 보호자 목록 조회 등 가능
                        """)
                .version("v1");
    }
}
