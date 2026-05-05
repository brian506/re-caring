package com.recaring.moremenu.controller;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.security.vo.TokenPayload;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.util.Date;

@DisplayName("더보기 메뉴 컨트롤러 HTTP 통합 테스트")
@Tag("integration")
class MoreMenuControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CareRelationshipRepository careRelationshipRepository;

    @Autowired
    private JwtGenerator jwtGenerator;

    private Member ward;
    private Member guardian;
    private Member manager;

    @BeforeEach
    void setUp() {
        ward = memberRepository.save(CareFixture.createWardMember("01010000001"));
        guardian = memberRepository.save(CareFixture.createGuardianMember("01010000002"));
        manager = memberRepository.save(CareFixture.createGuardianMember("01010000003"));
    }

    @AfterEach
    void tearDown() {
        careRelationshipRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("GET /api/v1/more/menu - 인증 없이 요청하면 401을 반환한다")
    void getMenu_without_auth_returns_401() {
        client.get()
                .uri("/api/v1/more/menu")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E401");
    }

    @Test
    @DisplayName("GET /api/v1/more/menu - 보호 대상자는 wardKey 없이 메뉴를 조회한다")
    void getMenu_returns_ward_menu() {
        client.get()
                .uri("/api/v1/more/menu")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(ward))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.contextType").isEqualTo("WARD")
                .jsonPath("$.data.sections[0].sectionKey").isEqualTo("SETTING")
                .jsonPath("$.data.sections[0].items[0].itemKey").isEqualTo("NOTIFICATION_SETTING")
                .jsonPath("$.data.sections[0].items[1].itemKey").isEqualTo("PROTECTOR_SETTING");
    }

    @Test
    @DisplayName("GET /api/v1/more/menu - 주보호자는 wardKey로 모든 설정이 활성화된 메뉴를 조회한다")
    void getMenu_returns_guardian_menu() {
        careRelationshipRepository.save(CareRelationship.of(
                ward.getMemberKey(), guardian.getMemberKey(), CareRole.GUARDIAN));

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/more/menu")
                        .queryParam("wardKey", ward.getMemberKey())
                        .build())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.contextType").isEqualTo("GUARDIAN")
                .jsonPath("$.data.sections[0].items[0].itemKey").isEqualTo("LOCATION_COLLECTION_SETTING")
                .jsonPath("$.data.sections[0].items[0].enabled").isEqualTo(true)
                .jsonPath("$.data.sections[0].items[2].itemKey").isEqualTo("SAFE_ZONE_SETTING")
                .jsonPath("$.data.sections[0].items[2].enabled").isEqualTo(true);
    }

    @Test
    @DisplayName("GET /api/v1/more/menu - 관리자는 위치 관련 설정이 비활성화된 메뉴를 조회한다")
    void getMenu_returns_manager_menu() {
        careRelationshipRepository.save(CareRelationship.of(
                ward.getMemberKey(), manager.getMemberKey(), CareRole.MANAGER));

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/more/menu")
                        .queryParam("wardKey", ward.getMemberKey())
                        .build())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(manager))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.contextType").isEqualTo("MANAGER")
                .jsonPath("$.data.sections[0].items[0].itemKey").isEqualTo("LOCATION_COLLECTION_SETTING")
                .jsonPath("$.data.sections[0].items[0].enabled").isEqualTo(false)
                .jsonPath("$.data.sections[0].items[2].itemKey").isEqualTo("SAFE_ZONE_SETTING")
                .jsonPath("$.data.sections[0].items[2].enabled").isEqualTo(false);
    }

    @Test
    @DisplayName("GET /api/v1/more/menu - 보호자는 wardKey가 없으면 400을 반환한다")
    void getMenu_guardian_without_ward_key_returns_400() {
        client.get()
                .uri("/api/v1/more/menu")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E5010");
    }

    @Test
    @DisplayName("GET /api/v1/more/menu - 보호자는 wardKey가 공백이면 400을 반환한다")
    void getMenu_guardian_with_blank_ward_key_returns_400() {
        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/more/menu")
                        .queryParam("wardKey", " ")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E5010");
    }

    @Test
    @DisplayName("GET /api/v1/more/menu - 관계 없는 wardKey면 403을 반환한다")
    void getMenu_guardian_with_unrelated_ward_key_returns_403() {
        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/more/menu")
                        .queryParam("wardKey", ward.getMemberKey())
                        .build())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E6001");
    }

    @Test
    @DisplayName("GET /v3/api-docs - Swagger 문서에 더보기 메뉴 API 계약이 노출된다")
    void openApi_docs_include_more_menu_operation() {
        client.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['/api/v1/more/menu'].get.summary").isEqualTo("Get more menu")
                .jsonPath("$.paths['/api/v1/more/menu'].get.parameters[0].name").isEqualTo("wardKey")
                .jsonPath("$.paths['/api/v1/more/menu'].get.parameters[0].required").isEqualTo(false)
                .jsonPath("$.components.securitySchemes.BearerAuth.scheme").isEqualTo("bearer");
    }

    @Test
    @DisplayName("GET /swagger-ui/index.html - Swagger UI에 접근할 수 있다")
    void swagger_ui_is_accessible() {
        client.get()
                .uri("/swagger-ui/index.html")
                .exchange()
                .expectStatus().isOk();
    }

    private String bearerToken(Member member) {
        return "Bearer " + jwtGenerator.generateJwt(
                new TokenPayload(member.getMemberKey(), member.getRole(), new Date())
        ).accessToken();
    }
}
