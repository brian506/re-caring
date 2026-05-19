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

@DisplayName("FcmDeviceTokenController HTTP integration test")
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
    @DisplayName("PUT /api/v1/notifications/device-tokens - guardian token is registered")
    void upsert_registers_guardian_token() {
        client.put()
                .uri("/api/v1/notifications/device-tokens")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "token": "guardian-fcm-token-http",
                          "recipientType": "GUARDIAN",
                          "platform": "ANDROID"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.memberKey").isEqualTo(guardian.getMemberKey())
                .jsonPath("$.data.recipientType").isEqualTo("GUARDIAN")
                .jsonPath("$.data.platform").isEqualTo("ANDROID")
                .jsonPath("$.data.active").isEqualTo(true);

        assertThat(fcmDeviceTokenRepository.findByToken("guardian-fcm-token-http"))
                .isPresent()
                .get()
                .satisfies(token -> {
                    assertThat(token.getMemberKey()).isEqualTo(guardian.getMemberKey());
                    assertThat(token.isActive()).isTrue();
                });
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/device-tokens - existing token is reassigned")
    void upsert_reassigns_existing_token() {
        client.put()
                .uri("/api/v1/notifications/device-tokens")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "token": "shared-fcm-token-http",
                          "recipientType": "GUARDIAN",
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
                          "recipientType": "MANAGER",
                          "platform": "IOS"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.memberKey").isEqualTo(manager.getMemberKey())
                .jsonPath("$.data.recipientType").isEqualTo("MANAGER")
                .jsonPath("$.data.platform").isEqualTo("IOS");

        assertThat(fcmDeviceTokenRepository.findAll()).hasSize(1);
        assertThat(fcmDeviceTokenRepository.findByToken("shared-fcm-token-http"))
                .isPresent()
                .get()
                .satisfies(token -> assertThat(token.getMemberKey()).isEqualTo(manager.getMemberKey()));
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/device-tokens - ward cannot register FCM recipient token")
    void upsert_returns_401_for_ward() {
        client.put()
                .uri("/api/v1/notifications/device-tokens")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(ward))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "token": "ward-fcm-token-http",
                          "recipientType": "GUARDIAN",
                          "platform": "ANDROID"
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/device-tokens - blank token returns 400")
    void upsert_returns_400_for_blank_token() {
        client.put()
                .uri("/api/v1/notifications/device-tokens")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "token": " ",
                          "recipientType": "GUARDIAN",
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
