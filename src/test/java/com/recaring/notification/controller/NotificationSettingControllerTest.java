package com.recaring.notification.controller;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.security.vo.TokenPayload;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Date;

@DisplayName("NotificationSettingController HTTP 통합 테스트")
class NotificationSettingControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CareRelationshipRepository careRelationshipRepository;
    @Autowired
    private NotificationSettingRepository notificationSettingRepository;
    @Autowired
    private JwtGenerator jwtGenerator;

    private Member ward;
    private Member guardian;
    private Member manager;
    private Member otherGuardian;

    @BeforeEach
    void setUp() {
        ward = memberRepository.save(NotificationFixture.createWard());
        guardian = memberRepository.save(NotificationFixture.createGuardian());
        manager = memberRepository.save(NotificationFixture.createManager());
        otherGuardian = memberRepository.save(NotificationFixture.createOtherGuardian());

        careRelationshipRepository.save(CareRelationship.of(
                ward.getMemberKey(),
                guardian.getMemberKey(),
                CareRole.GUARDIAN
        ));
        careRelationshipRepository.save(CareRelationship.of(
                ward.getMemberKey(),
                manager.getMemberKey(),
                CareRole.MANAGER
        ));
    }

    @AfterEach
    void tearDown() {
        notificationSettingRepository.deleteAllInBatch();
        careRelationshipRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("GET /api/v1/notifications/settings/{wardKey} - 저장된 설정이 없으면 기본값과 옵션을 조회한다")
    void getSetting_returns_default_and_options() {
        client.get()
                .uri("/api/v1/notifications/settings/{wardKey}", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.safeZone.entryEnabled").isEqualTo(true)
                .jsonPath("$.data.safeZone.exitEnabled").isEqualTo(true)
                .jsonPath("$.data.anomaly.routeDeviationEnabled").isEqualTo(true)
                .jsonPath("$.data.anomaly.speedAnomalyEnabled").isEqualTo(true)
                .jsonPath("$.data.anomaly.wanderingAnomalyEnabled").isEqualTo(true)
                .jsonPath("$.data.anomaly.sensitivity").isEqualTo("NORMAL")
                .jsonPath("$.data.anomaly.sensitivityOptions[0]").isEqualTo("VERY_LOW")
                .jsonPath("$.data.anomaly.sensitivityOptions[4]").isEqualTo("VERY_HIGH")
                .jsonPath("$.data.emergencyCall.enabled").isEqualTo(true)
                .jsonPath("$.data.battery.lowBatteryEnabled").isEqualTo(true)
                .jsonPath("$.data.battery.thresholdPercent").isEqualTo(25)
                .jsonPath("$.data.battery.thresholdOptions[0]").isEqualTo(10)
                .jsonPath("$.data.battery.thresholdOptions[18]").isEqualTo(100);
    }

    @Test
    @DisplayName("PATCH /safe-zone - 관계자가 수정한 공통 설정을 대상자 본인이 조회한다")
    void updateSafeZone_by_manager_is_visible_to_ward() {
        client.patch()
                .uri("/api/v1/notifications/settings/{wardKey}/safe-zone", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(manager))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"entryEnabled": false, "exitEnabled": true}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        client.get()
                .uri("/api/v1/notifications/settings/{wardKey}", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(ward))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.safeZone.entryEnabled").isEqualTo(false)
                .jsonPath("$.data.safeZone.exitEnabled").isEqualTo(true);
    }

    @Test
    @DisplayName("PATCH /anomaly - 대상자 본인이 이상탐지 알림 설정을 수정한다")
    void updateAnomaly_by_ward_updates_setting() {
        client.patch()
                .uri("/api/v1/notifications/settings/{wardKey}/anomaly", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(ward))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "routeDeviationEnabled": false,
                          "speedAnomalyEnabled": true,
                          "wanderingAnomalyEnabled": false,
                          "sensitivity": "VERY_HIGH"
                        }
                        """)
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/api/v1/notifications/settings/{wardKey}", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.anomaly.routeDeviationEnabled").isEqualTo(false)
                .jsonPath("$.data.anomaly.speedAnomalyEnabled").isEqualTo(true)
                .jsonPath("$.data.anomaly.wanderingAnomalyEnabled").isEqualTo(false)
                .jsonPath("$.data.anomaly.sensitivity").isEqualTo("VERY_HIGH");
    }

    @Test
    @DisplayName("PATCH /emergency-call - 주보호자가 응급호출 알림 설정을 수정한다")
    void updateEmergencyCall_by_guardian_updates_setting() {
        client.patch()
                .uri("/api/v1/notifications/settings/{wardKey}/emergency-call", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"enabled": false}
                        """)
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/api/v1/notifications/settings/{wardKey}", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(manager))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.emergencyCall.enabled").isEqualTo(false);
    }

    @Test
    @DisplayName("PATCH /battery - 배터리 알림 설정을 수정한다")
    void updateBattery_updates_setting() {
        client.patch()
                .uri("/api/v1/notifications/settings/{wardKey}/battery", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"lowBatteryEnabled": false, "thresholdPercent": 40}
                        """)
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/api/v1/notifications/settings/{wardKey}", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.battery.lowBatteryEnabled").isEqualTo(false)
                .jsonPath("$.data.battery.thresholdPercent").isEqualTo(40);
    }

    @Test
    @DisplayName("GET /api/v1/notifications/settings/{wardKey} - 케어 관계가 없으면 403을 반환한다")
    void getSetting_returns_403_for_unrelated_member() {
        client.get()
                .uri("/api/v1/notifications/settings/{wardKey}", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(otherGuardian))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E6001");
    }

    @Test
    @DisplayName("PATCH /anomaly - 지원하지 않는 민감도면 400을 반환한다")
    void updateAnomaly_returns_400_for_invalid_sensitivity() {
        client.patch()
                .uri("/api/v1/notifications/settings/{wardKey}/anomaly", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "routeDeviationEnabled": true,
                          "speedAnomalyEnabled": true,
                          "wanderingAnomalyEnabled": true,
                          "sensitivity": "INVALID"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E8000");
    }

    @Test
    @DisplayName("PATCH /battery - 지원하지 않는 배터리 임계값이면 400을 반환한다")
    void updateBattery_returns_400_for_invalid_threshold() {
        client.patch()
                .uri("/api/v1/notifications/settings/{wardKey}/battery", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"lowBatteryEnabled": true, "thresholdPercent": 12}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E8001");
    }

    @Test
    @DisplayName("GET /v3/api-docs - Swagger OpenAPI 문서에 알림 설정 API가 노출된다")
    void swaggerApiDocs_contains_notification_setting_paths() {
        client.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['/api/v1/notifications/settings/{wardKey}'].get.summary")
                .isEqualTo("Get notification settings")
                .jsonPath("$.paths['/api/v1/notifications/settings/{wardKey}/safe-zone'].patch.summary")
                .isEqualTo("Update safe zone notification settings")
                .jsonPath("$.paths['/api/v1/notifications/settings/{wardKey}/anomaly'].patch.summary")
                .isEqualTo("Update anomaly notification settings")
                .jsonPath("$.paths['/api/v1/notifications/settings/{wardKey}/emergency-call'].patch.summary")
                .isEqualTo("Update emergency call notification settings")
                .jsonPath("$.paths['/api/v1/notifications/settings/{wardKey}/battery'].patch.summary")
                .isEqualTo("Update battery notification settings");
    }

    @Test
    @DisplayName("GET /swagger-ui/index.html - Swagger UI에 접근할 수 있다")
    void swaggerUi_is_accessible() {
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
