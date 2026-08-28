package com.recaring.notification.controller;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.notification.dataaccess.repository.NotificationRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

@DisplayName("알림 목록 컨트롤러 HTTP 통합 테스트")
class NotificationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private Member guardian;

    @BeforeEach
    void setUp() {
        guardian = memberRepository.save(NotificationFixture.createGuardian());
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("로그인한 회원 본인에게 발송된 알림 목록을 반환한다")
    void getMyNotifications_returns_own_notifications() {
        // given
        notificationRepository.save(NotificationFixture.batteryLowNotification(guardian.getMemberKey()));

        // when / then
        client.get()
                .uri("/api/v1/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data[0].eventType").isEqualTo(NotificationFixture.BATTERY_LOW_EVENT_TYPE)
                .jsonPath("$.data[0].title").isEqualTo(NotificationFixture.BATTERY_LOW_TITLE)
                .jsonPath("$.data[0].body").isEqualTo(NotificationFixture.BATTERY_LOW_BODY)
                .jsonPath("$.data[0].dataPayload.type").isEqualTo(NotificationFixture.BATTERY_LOW_EVENT_TYPE);
    }

    @Test
    @DisplayName("다른 회원에게 발송된 알림은 목록에서 제외한다")
    void getMyNotifications_excludes_other_members_notifications() {
        // given
        Member otherGuardian = memberRepository.save(NotificationFixture.createOtherGuardian());
        notificationRepository.save(NotificationFixture.batteryLowNotification(otherGuardian.getMemberKey()));

        // when / then
        client.get()
                .uri("/api/v1/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isEmpty();
    }

    @Test
    @DisplayName("받은 알림이 없으면 빈 목록을 반환한다")
    void getMyNotifications_returns_empty_list_when_none() {
        client.get()
                .uri("/api/v1/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data").isEmpty();
    }

    @Test
    @DisplayName("인증 토큰이 없으면 401을 반환한다")
    void getMyNotifications_returns_401_without_token() {
        client.get()
                .uri("/api/v1/notifications")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
