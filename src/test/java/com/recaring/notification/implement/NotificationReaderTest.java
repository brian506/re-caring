package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.repository.NotificationRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.NotificationItem;
import com.recaring.notification.vo.NotificationSlice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 Reader 단위 테스트")
class NotificationReaderTest {

    private static final int SIZE = 2;

    @InjectMocks
    private NotificationReader notificationReader;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("수신자 기준 조회 결과를 id가 포함된 NotificationItem VO로 변환해 반환한다")
    void findByRecipient_maps_entities_to_vo() {
        Notification notification = NotificationFixture.notificationWithId(30L, NotificationFixture.GUARDIAN_KEY);
        given(notificationRepository.findSliceByRecipient(NotificationFixture.GUARDIAN_KEY, null, SIZE))
                .willReturn(List.of(notification));

        NotificationSlice slice = notificationReader.findByRecipient(NotificationFixture.GUARDIAN_KEY, null, SIZE);

        assertThat(slice.items()).hasSize(1);
        NotificationItem item = slice.items().getFirst();
        assertThat(item.id()).isEqualTo(30L);
        assertThat(item.eventType()).isEqualTo(NotificationFixture.BATTERY_LOW_EVENT_TYPE);
        assertThat(item.title()).isEqualTo(NotificationFixture.BATTERY_LOW_TITLE);
        assertThat(item.body()).isEqualTo(NotificationFixture.BATTERY_LOW_BODY);
        assertThat(item.dataPayload()).containsEntry("type", NotificationFixture.BATTERY_LOW_EVENT_TYPE);
        assertThat(item.notificationKey()).isEqualTo(notification.getNotificationKey());
    }

    @Test
    @DisplayName("요청 크기를 초과해 조회되면 초과분을 제외하고 다음 페이지가 있다고 알린다")
    void findByRecipient_excludes_overflow_from_items() {
        given(notificationRepository.findSliceByRecipient(NotificationFixture.GUARDIAN_KEY, 40L, SIZE))
                .willReturn(List.of(
                        NotificationFixture.notificationWithId(30L, NotificationFixture.GUARDIAN_KEY),
                        NotificationFixture.notificationWithId(20L, NotificationFixture.GUARDIAN_KEY),
                        NotificationFixture.notificationWithId(10L, NotificationFixture.GUARDIAN_KEY)
                ));

        NotificationSlice slice = notificationReader.findByRecipient(NotificationFixture.GUARDIAN_KEY, 40L, SIZE);

        assertThat(slice.items()).extracting(NotificationItem::id).containsExactly(30L, 20L);
        assertThat(slice.hasNext()).isTrue();
        assertThat(slice.nextCursor()).isEqualTo(20L);
    }

    @Test
    @DisplayName("알림이 없으면 빈 슬라이스를 반환한다")
    void findByRecipient_returns_empty_slice_when_none() {
        given(notificationRepository.findSliceByRecipient(NotificationFixture.GUARDIAN_KEY, null, SIZE))
                .willReturn(List.of());

        NotificationSlice slice = notificationReader.findByRecipient(NotificationFixture.GUARDIAN_KEY, null, SIZE);

        assertThat(slice.items()).isEmpty();
        assertThat(slice.hasNext()).isFalse();
        assertThat(slice.nextCursor()).isNull();
    }
}
