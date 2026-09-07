package com.recaring.notification.controller;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.repository.NotificationRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

@DisplayName("알림 목록 컨트롤러 HTTP 통합 테스트")
class NotificationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @DisplayName("첫 페이지는 최신 알림부터 요청한 개수만큼 반환한다")
    void getMyNotifications_returns_first_page_in_latest_order() {
        // given
        saveNotification(guardian.getMemberKey());
        Notification middle = saveNotification(guardian.getMemberKey());
        Notification newest = saveNotification(guardian.getMemberKey());

        // when / then
        client.get()
                .uri("/api/v1/notifications?size=2")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.items.length()").isEqualTo(2)
                .jsonPath("$.data.items[0].notificationKey").isEqualTo(newest.getNotificationKey())
                .jsonPath("$.data.items[1].notificationKey").isEqualTo(middle.getNotificationKey())
                .jsonPath("$.data.items[0].eventType").isEqualTo(NotificationFixture.BATTERY_LOW_EVENT_TYPE)
                .jsonPath("$.data.items[0].title").isEqualTo(NotificationFixture.BATTERY_LOW_TITLE)
                .jsonPath("$.data.items[0].body").isEqualTo(NotificationFixture.BATTERY_LOW_BODY)
                .jsonPath("$.data.items[0].dataPayload.type").isEqualTo(NotificationFixture.BATTERY_LOW_EVENT_TYPE)
                .jsonPath("$.data.hasNext").isEqualTo(true)
                .jsonPath("$.data.nextCursor").isEqualTo(middle.getId());
    }

    @Test
    @DisplayName("최신순 정렬은 생성 시각이 아니라 알림 식별자를 기준으로 한다")
    void getMyNotifications_orders_by_id_not_created_at() {
        // given — 식별자 순서와 생성 시각 순서를 서로 반대로 만든다
        Notification first = saveNotification(guardian.getMemberKey());
        Notification second = saveNotification(guardian.getMemberKey());
        Notification third = saveNotification(guardian.getMemberKey());
        overwriteCreatedAt(first, LocalDateTime.of(2026, 9, 8, 12, 0));
        overwriteCreatedAt(second, LocalDateTime.of(2026, 9, 8, 11, 0));
        overwriteCreatedAt(third, LocalDateTime.of(2026, 9, 8, 10, 0));

        // when / then
        client.get()
                .uri("/api/v1/notifications?size=3")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.items[0].notificationKey").isEqualTo(third.getNotificationKey())
                .jsonPath("$.data.items[1].notificationKey").isEqualTo(second.getNotificationKey())
                .jsonPath("$.data.items[2].notificationKey").isEqualTo(first.getNotificationKey());
    }

    @Test
    @DisplayName("직전 응답의 커서로 요청하면 그 뒤 알림만 이어서 반환한다")
    void getMyNotifications_continues_after_cursor() {
        // given
        Notification oldest = saveNotification(guardian.getMemberKey());
        Notification middle = saveNotification(guardian.getMemberKey());
        saveNotification(guardian.getMemberKey());

        // when / then
        client.get()
                .uri("/api/v1/notifications?cursor=" + middle.getId() + "&size=2")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.items.length()").isEqualTo(1)
                .jsonPath("$.data.items[0].notificationKey").isEqualTo(oldest.getNotificationKey())
                .jsonPath("$.data.hasNext").isEqualTo(false)
                .jsonPath("$.data.nextCursor").isEmpty();
    }

    @Test
    @DisplayName("남은 알림이 요청 개수와 같으면 마지막 페이지로 응답한다")
    void getMyNotifications_marks_last_page_when_remaining_equals_size() {
        // given
        saveNotification(guardian.getMemberKey());
        saveNotification(guardian.getMemberKey());

        // when / then
        client.get()
                .uri("/api/v1/notifications?size=2")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.items.length()").isEqualTo(2)
                .jsonPath("$.data.hasNext").isEqualTo(false)
                .jsonPath("$.data.nextCursor").isEmpty();
    }

    @Test
    @DisplayName("페이지 크기를 생략하면 10건을 반환한다")
    void getMyNotifications_uses_default_size_of_ten() {
        // given
        IntStream.rangeClosed(1, 11).forEach(i -> saveNotification(guardian.getMemberKey()));

        // when / then
        client.get()
                .uri("/api/v1/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.items.length()").isEqualTo(10)
                .jsonPath("$.data.hasNext").isEqualTo(true);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50})
    @DisplayName("페이지 크기가 1 이상 50 이하면 조회에 성공한다")
    void getMyNotifications_accepts_size_within_allowed_range(int size) {
        // given
        saveNotification(guardian.getMemberKey());

        // when / then
        client.get()
                .uri("/api/v1/notifications?size=" + size)
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.items.length()").isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 51})
    @DisplayName("페이지 크기가 1 미만이거나 50을 넘으면 400을 반환한다")
    void getMyNotifications_rejects_size_out_of_range(int size) {
        client.get()
                .uri("/api/v1/notifications?size=" + size)
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("ERROR")
                .jsonPath("$.error.errorCode").isEqualTo("E400");
    }

    @Test
    @DisplayName("다른 회원에게 발송된 알림은 목록에서 제외한다")
    void getMyNotifications_excludes_other_members_notifications() {
        // given
        Member otherGuardian = memberRepository.save(NotificationFixture.createOtherGuardian());
        saveNotification(otherGuardian.getMemberKey());

        // when / then
        client.get()
                .uri("/api/v1/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.items").isEmpty()
                .jsonPath("$.data.hasNext").isEqualTo(false);
    }

    @Test
    @DisplayName("다른 회원의 알림을 가리키는 커서로 요청해도 본인 알림만 반환한다")
    void getMyNotifications_returns_only_own_notifications_for_foreign_cursor() {
        // given
        Member otherGuardian = memberRepository.save(NotificationFixture.createOtherGuardian());
        Notification mine = saveNotification(guardian.getMemberKey());
        Notification others = saveNotification(otherGuardian.getMemberKey());
        saveNotification(guardian.getMemberKey());

        // when / then
        client.get()
                .uri("/api/v1/notifications?cursor=" + others.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.items.length()").isEqualTo(1)
                .jsonPath("$.data.items[0].notificationKey").isEqualTo(mine.getNotificationKey());
    }

    @Test
    @DisplayName("받은 알림이 없으면 빈 목록과 마지막 페이지를 반환한다")
    void getMyNotifications_returns_empty_slice_when_none() {
        client.get()
                .uri("/api/v1/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.items").isEmpty()
                .jsonPath("$.data.hasNext").isEqualTo(false)
                .jsonPath("$.data.nextCursor").isEmpty();
    }

    @Test
    @DisplayName("응답 항목에 DB 식별자를 노출하지 않는다")
    void getMyNotifications_does_not_expose_primary_key_in_items() {
        // given
        saveNotification(guardian.getMemberKey());

        // when / then
        client.get()
                .uri("/api/v1/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.items[0].notificationKey").exists()
                .jsonPath("$.data.items[0].id").doesNotExist();
    }

    @Test
    @DisplayName("인증 토큰이 없으면 401을 반환한다")
    void getMyNotifications_returns_401_without_token() {
        client.get()
                .uri("/api/v1/notifications")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private Notification saveNotification(String recipientMemberKey) {
        return notificationRepository.saveAndFlush(NotificationFixture.batteryLowNotification(recipientMemberKey));
    }

    private void overwriteCreatedAt(Notification notification, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE notifications SET created_at = ? WHERE notification_id = ?",
                createdAt, notification.getId()
        );
    }
}
