package com.recaring.notification.implement.listener;

import com.recaring.inquiry.fixture.InquiryFixture;
import com.recaring.notification.implement.NotificationSendManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("문의 답변 알림 리스너 단위 테스트")
class InquiryNotificationListenerTest {

    private static final String EVENT_TYPE = "INQUIRY_ANSWERED";

    @InjectMocks
    private InquiryNotificationListener inquiryNotificationListener;

    @Mock
    private NotificationSendManager notificationSendManager;

    @Test
    @DisplayName("답변이 등록되면 작성자에게 문의 제목을 담은 답변 알림을 보낸다")
    void sends_answered_notification_to_author() {
        inquiryNotificationListener.onInquiryAnswered(InquiryFixture.createAnsweredEvent());

        then(notificationSendManager).should().sendToMember(
                InquiryFixture.MEMBER_KEY,
                EVENT_TYPE,
                "문의에 답변이 등록되었어요",
                "\"알림이 안 와요\" 문의에 답변이 달렸어요.",
                Map.of("type", EVENT_TYPE, "inquiryKey", InquiryFixture.INQUIRY_KEY)
        );
    }
}
