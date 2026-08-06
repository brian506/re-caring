package com.recaring.notification.implement.listener;

import com.recaring.care.event.CareInvitationAcceptedEvent;
import com.recaring.care.event.CareInvitationSentEvent;
import com.recaring.member.implement.MemberReader;
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
public class CareInvitationNotificationListener {

    private static final String EVENT_TYPE_SENT = "CARE_INVITATION_SENT";
    private static final String EVENT_TYPE_ACCEPTED = "CARE_INVITATION_ACCEPTED";

    private final NotificationSendManager notificationSendManager;
    private final MemberReader memberReader;

    @Async("broadcastExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCareInvitationSent(CareInvitationSentEvent event) {
        log.info("[케어 초대 알림 : 발송 이벤트 수신]: requestKey={} | targetMemberKey={}",
                event.requestKey(), event.targetMemberKey());

        String requesterName = memberReader.findByMemberKey(event.requesterMemberKey()).getName();
        String roleDescription = event.targetRole().getDescription();

        notificationSendManager.sendToMember(
                event.targetMemberKey(),
                EVENT_TYPE_SENT,
                "새로운 " + roleDescription + " 요청",
                requesterName + "님이 " + roleDescription + "로 요청을 보냈어요.",
                Map.of("type", EVENT_TYPE_SENT, "requestKey", event.requestKey(), "role", event.targetRole().name())
        );
    }

    @Async("broadcastExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCareInvitationAccepted(CareInvitationAcceptedEvent event) {
        log.info("[케어 초대 알림 : 수락 이벤트 수신]: requestKey={} | requesterMemberKey={}",
                event.requestKey(), event.requesterMemberKey());

        String acceptorName = memberReader.findByMemberKey(event.acceptorMemberKey()).getName();

        notificationSendManager.sendToMember(
                event.requesterMemberKey(),
                EVENT_TYPE_ACCEPTED,
                "요청 수락",
                acceptorName + "님이 요청을 수락했어요.",
                Map.of("type", EVENT_TYPE_ACCEPTED, "requestKey", event.requestKey(), "role", event.targetRole().name())
        );
    }
}
