package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.repository.NotificationRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.NotificationItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 Reader 단위 테스트")
class NotificationReaderTest {

    @InjectMocks
    private NotificationReader notificationReader;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("수신자 기준 조회 결과를 NotificationItem VO로 변환해 반환한다")
    void findByRecipient_maps_entities_to_vo() {
        Notification notification = NotificationFixture.notification(
                NotificationFixture.GUARDIAN_KEY, "BATTERY_LOW", "배터리 부족", "배터리가 부족합니다. 잔량은 40% 입니다.");
        given(notificationRepository.findByRecipientMemberKeyOrderByCreatedAtDesc(NotificationFixture.GUARDIAN_KEY))
                .willReturn(List.of(notification));

        List<NotificationItem> result = notificationReader.findByRecipient(NotificationFixture.GUARDIAN_KEY);

        assertThat(result).hasSize(1);
        NotificationItem item = result.get(0);
        assertThat(item.eventType()).isEqualTo("BATTERY_LOW");
        assertThat(item.title()).isEqualTo("배터리 부족");
        assertThat(item.body()).isEqualTo("배터리가 부족합니다. 잔량은 40% 입니다.");
        assertThat(item.dataPayload()).containsEntry("type", "BATTERY_LOW");
        assertThat(item.notificationKey()).isNotBlank();
        then(notificationRepository).should().findByRecipientMemberKeyOrderByCreatedAtDesc(NotificationFixture.GUARDIAN_KEY);
    }

    @Test
    @DisplayName("알림이 없으면 빈 리스트를 반환한다")
    void findByRecipient_returns_empty_list_when_none() {
        given(notificationRepository.findByRecipientMemberKeyOrderByCreatedAtDesc(NotificationFixture.GUARDIAN_KEY))
                .willReturn(List.of());

        List<NotificationItem> result = notificationReader.findByRecipient(NotificationFixture.GUARDIAN_KEY);

        assertThat(result).isEmpty();
    }
}
