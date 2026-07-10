package com.recaring.notification.controller.response;

import com.recaring.notification.vo.NotificationItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationResponse 단위 테스트")
class NotificationResponseTest {

    @Test
    @DisplayName("from() - NotificationItem의 createdAt(LocalDateTime)을 시스템 기본 시간대의 Instant로 변환한다")
    void from_converts_createdAt_to_instant() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 9, 12, 0, 0);
        NotificationItem item = new NotificationItem(
                "notification-key", "SAFE_ZONE_EXIT", "title", "body", Map.of(), createdAt);

        NotificationResponse response = NotificationResponse.from(item);

        assertThat(response.createdAt()).isEqualTo(createdAt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
