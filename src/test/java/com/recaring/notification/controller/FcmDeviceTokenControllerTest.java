package com.recaring.notification.controller;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import com.recaring.notification.fixture.NotificationFixture;
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
import org.springframework.http.MediaType;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FCM 디바이스 토큰 컨트롤러 HTTP 통합 테스트")
@Tag("integration")
class FcmDeviceTokenControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private FcmDeviceTokenRepository fcmDeviceTokenRepository;
    @Autowired
    private JwtGenerator jwtGenerator;

    private Member guardian;
    private Member manager;
    private Member ward;

    @BeforeEach
    void setUp() {
        guardian = memberRepository.save(NotificationFixture.createGuardian());
        manager = memberRepository.save(NotificationFixture.createManager());
        ward = memberRepository.save(NotificationFixture.createWard());
    }

    @AfterEach
    void tearDown() {
        fcmDeviceTokenRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("보호자 FCM 토큰을 등록한다")
    void upsert_registers_guardian_token() {
        client.put()
                .uri("/api/v1/notifications/device-tokens")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "token": "guardian-fcm-token-http",
                          "careRole": "GUARDIAN",
                          "platform": "ANDROID"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.memberKey").isEqualTo(guardian.getMemberKey())
                .jsonPath("$.data.careRole").isEqualTo("GUARDIAN")
                .jsonPath("$.data.platform").isEqualTo("ANDROID");

        assertThat(fcmDeviceTokenRepository.findByToken("guardian-fcm-token-http"))
                .isPresent()
                .get()
                .satisfies(token -> {
                    assertThat(token.getMemberKey()).isEqualTo(guardian.getMemberKey());
                });
    }

    @Test
    @DisplayName("기존 FCM 토큰을 다른 수신자에게 재할당한다")
    void upsert_reassigns_existing_token() {
        client.put()
                .uri("/api/v1/notifications/device-tokens")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "token": "shared-fcm-token-http",
                          "careRole": "GUARDIAN",
                          "platform": "ANDROID"
                        }
                        """)
                .exchange()
                .expectStatus().isOk();

        client.put()
                .uri("/api/v1/notifications/device-tokens")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(manager))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "token": "shared-fcm-token-http",
                          "careRole": "MANAGER",
                          "platform": "IOS"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.memberKey").isEqualTo(manager.getMemberKey())
                .jsonPath("$.data.careRole").isEqualTo("MANAGER")
                .jsonPath("$.data.platform").isEqualTo("IOS");

        assertThat(fcmDeviceTokenRepository.findAll()).hasSize(1);
        assertThat(fcmDeviceTokenRepository.findByToken("shared-fcm-token-http"))
                .isPresent()
                .get()
                .satisfies(token -> assertThat(token.getMemberKey()).isEqualTo(manager.getMemberKey()));
    }

    @Test
    @DisplayName("피보호자도 본인 대상 알림 수신을 위해 FCM 토큰을 등록할 수 있다")
    void upsert_registers_ward_token() {
        client.put()
                .uri("/api/v1/notifications/device-tokens")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(ward))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "token": "ward-fcm-token-http",
                          "careRole": "WARD",
                          "platform": "ANDROID"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.memberKey").isEqualTo(ward.getMemberKey())
                .jsonPath("$.data.careRole").isEqualTo("WARD");

        assertThat(fcmDeviceTokenRepository.findByToken("ward-fcm-token-http")).isPresent();
    }

    @Test
    @DisplayName("빈 FCM 토큰이면 400을 반환한다")
    void upsert_returns_400_for_blank_token() {
        client.put()
                .uri("/api/v1/notifications/device-tokens")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "token": " ",
                          "careRole": "GUARDIAN",
                          "platform": "ANDROID"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    private String bearerToken(Member member) {
        return "Bearer " + jwtGenerator.generateJwt(
                new TokenPayload(member.getMemberKey(), member.getRole(), new Date())
        ).accessToken();
    }
}
