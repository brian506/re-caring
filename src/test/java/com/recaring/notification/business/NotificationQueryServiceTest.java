package com.recaring.notification.business;

import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.NotificationReader;
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
@DisplayName("알림 조회 서비스 단위 테스트")
class NotificationQueryServiceTest {

    @InjectMocks
    private NotificationQueryService notificationQueryService;

    @Mock
    private NotificationReader notificationReader;

    @Test
    @DisplayName("Reader의 수신자별 조회 결과를 그대로 반환한다")
    void getMyNotifications_returns_reader_result() {
        List<NotificationItem> items = List.of(
                NotificationFixture.notificationItem("BATTERY_LOW", "배터리 부족", "배터리가 부족합니다. 잔량은 40% 입니다."),
                NotificationFixture.notificationItem("ROUTE_DEVIATION", "경로 이탈", "지정된 경로에서 이탈했습니다.")
        );
        given(notificationReader.findByRecipient(NotificationFixture.GUARDIAN_KEY)).willReturn(items);

        List<NotificationItem> result = notificationQueryService.getMyNotifications(NotificationFixture.GUARDIAN_KEY);

        assertThat(result).isEqualTo(items);
        then(notificationReader).should().findByRecipient(NotificationFixture.GUARDIAN_KEY);
    }
}
