package com.recaring.notification.vo;

import com.recaring.notification.fixture.NotificationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("알림 페이징 슬라이스 단위 테스트")
class NotificationSliceTest {

    private static final int SIZE = 2;

    @Test
    @DisplayName("요청 크기보다 많이 조회되면 초과분을 잘라내고 다음 페이지가 있다고 알린다")
    void of_truncates_overflow_and_marks_has_next() {
        List<NotificationItem> fetched = List.of(
                NotificationFixture.notificationItem(30L),
                NotificationFixture.notificationItem(20L),
                NotificationFixture.notificationItem(10L)
        );

        NotificationSlice slice = NotificationSlice.of(fetched, SIZE);

        assertThat(slice.items()).extracting(NotificationItem::id).containsExactly(30L, 20L);
        assertThat(slice.hasNext()).isTrue();
    }

    @Test
    @DisplayName("다음 페이지가 있으면 커서는 잘라낸 뒤 마지막 항목의 id다")
    void of_uses_last_remaining_item_id_as_next_cursor() {
        List<NotificationItem> fetched = List.of(
                NotificationFixture.notificationItem(30L),
                NotificationFixture.notificationItem(20L),
                NotificationFixture.notificationItem(10L)
        );

        NotificationSlice slice = NotificationSlice.of(fetched, SIZE);

        assertThat(slice.nextCursor()).isEqualTo(20L);
    }

    @Test
    @DisplayName("조회 건수가 요청 크기와 같으면 마지막 페이지로 본다")
    void of_marks_last_page_when_fetched_equals_size() {
        List<NotificationItem> fetched = List.of(
                NotificationFixture.notificationItem(30L),
                NotificationFixture.notificationItem(20L)
        );

        NotificationSlice slice = NotificationSlice.of(fetched, SIZE);

        assertThat(slice.items()).extracting(NotificationItem::id).containsExactly(30L, 20L);
        assertThat(slice.hasNext()).isFalse();
        assertThat(slice.nextCursor()).isNull();
    }

    @Test
    @DisplayName("조회 건수가 요청 크기보다 적으면 마지막 페이지로 본다")
    void of_marks_last_page_when_fetched_less_than_size() {
        List<NotificationItem> fetched = List.of(NotificationFixture.notificationItem(30L));

        NotificationSlice slice = NotificationSlice.of(fetched, SIZE);

        assertThat(slice.items()).extracting(NotificationItem::id).containsExactly(30L);
        assertThat(slice.hasNext()).isFalse();
        assertThat(slice.nextCursor()).isNull();
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 목록과 마지막 페이지를 반환한다")
    void of_returns_empty_slice_when_nothing_fetched() {
        NotificationSlice slice = NotificationSlice.of(List.of(), SIZE);

        assertThat(slice.items()).isEmpty();
        assertThat(slice.hasNext()).isFalse();
        assertThat(slice.nextCursor()).isNull();
    }
}
