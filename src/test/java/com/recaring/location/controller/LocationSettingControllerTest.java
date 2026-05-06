package com.recaring.location.controller;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.device.dataaccess.entity.WardDeviceToken;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.location.dataaccess.repository.LocationSettingRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
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

@DisplayName("LocationSettingController HTTP 통합 테스트")
class LocationSettingControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CareRelationshipRepository careRelationshipRepository;
    @Autowired
    private LocationSettingRepository locationSettingRepository;
    @Autowired
    private WardDeviceTokenRepository wardDeviceTokenRepository;
    @Autowired
    private JwtGenerator jwtGenerator;

    private Member ward;
    private Member guardian;
    private Member manager;
    private String wardDeviceToken;

    @BeforeEach
    void setUp() {
        ward = memberRepository.save(LocationFixture.createWard());
        guardian = memberRepository.save(LocationFixture.createGuardian());
        manager = memberRepository.save(LocationFixture.createManager());

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

        WardDeviceToken deviceToken = wardDeviceTokenRepository.save(WardDeviceToken.builder()
                .wardKey(ward.getMemberKey())
                .build());
        wardDeviceToken = deviceToken.getToken();
    }

    @AfterEach
    void tearDown() {
        locationSettingRepository.deleteAllInBatch();
        careRelationshipRepository.deleteAllInBatch();
        wardDeviceTokenRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("GET /api/v1/location/settings/{wardKey}/collection-interval - 기본값과 옵션을 조회한다")
    void getCollectionInterval_returns_default_and_options() {
        client.get()
                .uri("/api/v1/location/settings/{wardKey}/collection-interval", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.currentIntervalSeconds").isEqualTo(5)
                .jsonPath("$.data.defaultIntervalSeconds").isEqualTo(5)
                .jsonPath("$.data.options[0]").isEqualTo(5)
                .jsonPath("$.data.options[1]").isEqualTo(10)
                .jsonPath("$.data.options[2]").isEqualTo(30)
                .jsonPath("$.data.options[3]").isEqualTo(60)
                .jsonPath("$.data.options[4]").isEqualTo(180)
                .jsonPath("$.data.options[5]").isEqualTo(300);
    }

    @Test
    @DisplayName("PATCH /api/v1/location/settings/{wardKey}/collection-interval - 주보호자가 위치 수집 주기를 수정한다")
    void updateCollectionInterval_updates_interval_for_guardian() {
        client.patch()
                .uri("/api/v1/location/settings/{wardKey}/collection-interval", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"intervalSeconds": 30}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        client.get()
                .uri("/api/v1/location/settings/{wardKey}/collection-interval", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.currentIntervalSeconds").isEqualTo(30);
    }

    @Test
    @DisplayName("PATCH /api/v1/location/settings/{wardKey}/collection-interval - 허용되지 않은 주기면 400을 반환한다")
    void updateCollectionInterval_returns_400_for_unsupported_interval() {
        client.patch()
                .uri("/api/v1/location/settings/{wardKey}/collection-interval", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"intervalSeconds": 120}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E6002");
    }

    @Test
    @DisplayName("PATCH /api/v1/location/settings/{wardKey}/collection-interval - 관리자는 수정할 수 없다")
    void updateCollectionInterval_returns_403_for_manager() {
        client.patch()
                .uri("/api/v1/location/settings/{wardKey}/collection-interval", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(manager))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"intervalSeconds": 30}
                        """)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E5002");
    }

    @Test
    @DisplayName("GET /api/v1/location/settings/collection-interval/me - 보호 대상자 앱은 현재 주기만 조회한다")
    void getMyCollectionInterval_returns_current_interval_only_for_ward_device() {
        client.patch()
                .uri("/api/v1/location/settings/{wardKey}/collection-interval", ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"intervalSeconds": 60}
                        """)
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/api/v1/location/settings/collection-interval/me")
                .header(HttpHeaders.AUTHORIZATION, "Device " + wardDeviceToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.currentIntervalSeconds").isEqualTo(60)
                .jsonPath("$.data.options").doesNotExist();
    }

    private String bearerToken(Member member) {
        return "Bearer " + jwtGenerator.generateJwt(
                new TokenPayload(member.getMemberKey(), member.getRole(), new Date())
        ).accessToken();
    }
}
