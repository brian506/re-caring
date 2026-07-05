package com.recaring.notification.business;

import com.recaring.notification.business.command.NotificationSendCommand;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.NotificationSendManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 발송 서비스 단위 테스트")
class NotificationSendServiceTest {

    @InjectMocks
    private NotificationSendService notificationSendService;

    @Mock
    private NotificationSendManager notificationSendManager;

    @Test
    @DisplayName("브로드캐스트 - 커맨드를 풀어 매니저의 케어 파티 전송에 위임한다")
    void send_delegates_to_manager() {
        NotificationSendCommand command = NotificationFixture.sendCommand();

        notificationSendService.send(command);

        then(notificationSendManager).should().sendToCareParties(
                command.guardianMemberKeys(),
                command.managerMemberKeys(),
                command.eventType(),
                command.title(),
                command.body(),
                command.dataPayload()
        );
    }
}
