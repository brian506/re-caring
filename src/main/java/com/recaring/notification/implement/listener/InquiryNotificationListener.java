package com.recaring.notification.implement.listener;

import com.recaring.inquiry.event.InquiryAnsweredEvent;
import com.recaring.notification.implement.NotificationSendManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InquiryNotificationListener {

    private static final String EVENT_TYPE_ANSWERED = "INQUIRY_ANSWERED";
    private static final String TITLE = "문의에 답변이 등록되었어요";

    private final NotificationSendManager notificationSendManager;

    @Async("broadcastExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInquiryAnswered(InquiryAnsweredEvent event) {
        notificationSendManager.sendToMember(
                event.memberKey(),
                EVENT_TYPE_ANSWERED,
                TITLE,
                "\"" + event.title() + "\" 문의에 답변이 달렸어요.",
                Map.of("type", EVENT_TYPE_ANSWERED, "inquiryKey", event.inquiryKey())
        );
        log.info("[InquiryAnswerAlert : Sent]: inquiryKey={} | memberKey={}", event.inquiryKey(), event.memberKey());
    }
}
