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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.util.Date;
import org.springframework.http.MediaType;

@DisplayName("NotificationSettingController HTTP 통합 테스트")
class NotificationSettingControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private JwtGenerator jwtGenerator;
    @Autowired
    private CareRelationshipRepository careRelationshipRepository;
    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

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
                CareRole.PRIMARY_GUARDIAN
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
                .jsonPath("$.data.anomaly.speedAnomalyEnabled").isEqualTo(true)
                .jsonPath("$.data.anomaly.wanderingAnomalyEnabled").isEqualTo(true)
                .jsonPath("$.data.anomaly.abnormalDwellingEnabled").isEqualTo(true)
                .jsonPath("$.data.anomaly.routeDeviationEnabled").isEqualTo(true)
                .jsonPath("$.data.anomaly.timeAnomalyEnabled").isEqualTo(true)
                .jsonPath("$.data.emergencyCall.enabled").isEqualTo(true)
                .jsonPath("$.data.battery.lowBatteryEnabled").isEqualTo(true)
                .jsonPath("$.data.battery.thresholdPercents").isEmpty()
                .jsonPath("$.data.battery.thresholdOptions[0]").isEqualTo(10)
                .jsonPath("$.data.battery.thresholdOptions[9]").isEqualTo(100);
    }

    @Test
    @DisplayName("PATCH /safe-zone - 관계자가 수정한 공통 설정을 대상자 본인이 조회한다")
    void updateSafeZone_by_manager_is_visible_to_ward() {
        // 기본값이 둘 다 true라, 먼저 둘 다 끄지 않으면 exit가 기본값과 같아 무엇도 검증되지 않는다
        patchSafeZone(false, false);
        expectSafeZone(false, false);

        // 서로 다른 값으로 다시 저장해 두 필드가 뒤바뀌지 않는지 확인한다
        patchSafeZone(true, false);
        expectSafeZone(true, false);
    }

    private void patchSafeZone(boolean entryEnabled, boolean exitEnabled) {
        client.patch()
                .uri("/api/v1/notifications/settings/{wardKey}/safe-zone", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(manager))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"entryEnabled": %s, "exitEnabled": %s}
                        """.formatted(entryEnabled, exitEnabled))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");
    }

    private void expectSafeZone(boolean entryEnabled, boolean exitEnabled) {
        client.get()
                .uri("/api/v1/notifications/settings/{wardKey}", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(ward))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.safeZone.entryEnabled").isEqualTo(entryEnabled)
                .jsonPath("$.data.safeZone.exitEnabled").isEqualTo(exitEnabled);
    }

    @ParameterizedTest(name = "{0}번 토글만 끄면 그 유형만 꺼진 채로 조회된다")
    @ValueSource(ints = {0, 1, 2, 3, 4})
    @DisplayName("PATCH /anomaly - 대상자 본인이 끈 탐지 유형만 정확히 그 유형으로 저장된다")
    void updateAnomaly_by_ward_disables_only_that_detection_type(int disabledIndex) {
        // 한 자리만 false로 두면 컬럼이 뒤바뀌어 저장될 때 조회 결과가 어긋난다
        boolean[] toggles = {true, true, true, true, true};
        toggles[disabledIndex] = false;

        client.patch()
                .uri("/api/v1/notifications/settings/{wardKey}/anomaly", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(ward))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "speedAnomalyEnabled": %s,
                          "wanderingAnomalyEnabled": %s,
                          "abnormalDwellingEnabled": %s,
                          "routeDeviationEnabled": %s,
                          "timeAnomalyEnabled": %s
                        }
                        """.formatted(toggles[0], toggles[1], toggles[2], toggles[3], toggles[4]))
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/api/v1/notifications/settings/{wardKey}", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.anomaly.speedAnomalyEnabled").isEqualTo(toggles[0])
                .jsonPath("$.data.anomaly.wanderingAnomalyEnabled").isEqualTo(toggles[1])
                .jsonPath("$.data.anomaly.abnormalDwellingEnabled").isEqualTo(toggles[2])
                .jsonPath("$.data.anomaly.routeDeviationEnabled").isEqualTo(toggles[3])
                .jsonPath("$.data.anomaly.timeAnomalyEnabled").isEqualTo(toggles[4]);
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
                        {"lowBatteryEnabled": false, "thresholdPercents": [40, 90]}
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
                .jsonPath("$.data.battery.thresholdPercents[0]").isEqualTo(40)
                .jsonPath("$.data.battery.thresholdPercents[1]").isEqualTo(90);
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
    @DisplayName("PATCH /anomaly - 토글이 하나라도 빠지면 400을 반환한다")
    void updateAnomaly_returns_400_when_a_toggle_is_missing() {
        client.patch()
                .uri("/api/v1/notifications/settings/{wardKey}/anomaly", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "speedAnomalyEnabled": true,
                          "wanderingAnomalyEnabled": true,
                          "abnormalDwellingEnabled": true,
                          "routeDeviationEnabled": true
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E400");
    }

    @Test
    @DisplayName("PATCH /battery - 지원하지 않는 배터리 임계값이면 400을 반환한다")
    void updateBattery_returns_400_for_invalid_threshold() {
        client.patch()
                .uri("/api/v1/notifications/settings/{wardKey}/battery", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"lowBatteryEnabled": true, "thresholdPercents": [12]}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E9001");
    }

    private String bearerToken(Member member) {
        return "Bearer " + jwtGenerator.generateJwt(
                new TokenPayload(member.getMemberKey(), member.getRole(), new Date())
        ).accessToken();
    }
}
