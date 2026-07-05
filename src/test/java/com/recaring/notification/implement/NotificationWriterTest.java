package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.repository.NotificationRepository;
import com.recaring.notification.fixture.NotificationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 Writer 단위 테스트")
class NotificationWriterTest {

    @InjectMocks
    private NotificationWriter notificationWriter;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("수신자별로 알림을 한 건씩 저장한다")
    void addAll_saves_one_notification_per_recipient() {
        notificationWriter.addAll(
                List.of(NotificationFixture.GUARDIAN_KEY, NotificationFixture.MANAGER_KEY),
                "SAFE_ZONE_EXIT",
                "안심존 이탈",
                "홍길동님이 안심존을 벗어났어요.",
                Map.of("wardKey", NotificationFixture.WARD_KEY)
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        then(notificationRepository).should().saveAll(captor.capture());

        List<Notification> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(Notification::getRecipientMemberKey)
                .containsExactly(NotificationFixture.GUARDIAN_KEY, NotificationFixture.MANAGER_KEY);
        assertThat(saved).allSatisfy(notification -> {
            assertThat(notification.getEventType()).isEqualTo("SAFE_ZONE_EXIT");
            assertThat(notification.getTitle()).isEqualTo("안심존 이탈");
            assertThat(notification.getBody()).isEqualTo("홍길동님이 안심존을 벗어났어요.");
            assertThat(notification.getNotificationKey()).isNotBlank();
        });
    }

    @Test
    @DisplayName("중복 수신자는 한 번만 저장한다")
    void addAll_deduplicates_recipients() {
        notificationWriter.addAll(
                List.of(NotificationFixture.GUARDIAN_KEY, NotificationFixture.GUARDIAN_KEY),
                "CARE_INVITATION_SENT",
                "새로운 케어 요청",
                "본문",
                Map.of()
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        then(notificationRepository).should().saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getRecipientMemberKey())
                .isEqualTo(NotificationFixture.GUARDIAN_KEY);
    }
}
