package com.recaring.notification.implement;

import com.recaring.notification.implement.fcm.FcmClient;
import com.recaring.notification.implement.fcm.FcmDeviceTokenManager;
import com.recaring.notification.implement.fcm.FcmDeviceTokenReader;
import com.recaring.notification.implement.fcm.FcmPushMessage;
import com.recaring.notification.vo.FcmDeviceTokenInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSendManager {

    private final FcmDeviceTokenReader fcmDeviceTokenReader;
    private final FcmDeviceTokenManager fcmDeviceTokenManager;
    private final NotificationWriter notificationWriter;
    private final FcmClient fcmClient;

    public void sendToCaregivers(
            Collection<String> recipientMemberKeys,
            String eventType,
            String title,
            String body,
            Map<String, String> dataPayload
    ) {
        List<String> recipients = recipientMemberKeys.stream().distinct().toList();
        notificationWriter.addAll(recipients, eventType, title, body, dataPayload);

        List<FcmDeviceTokenInfo> tokens = fcmDeviceTokenReader.findGuardianTokens(recipients);
        dispatch(tokens, FcmPushMessage.notification(title, body, dataPayload));
    }

    public void sendDataOnlyToCaregivers(
            Collection<String> recipientMemberKeys,
            String eventType,
            String title,
            String body,
            Map<String, String> dataPayload
    ) {
        List<String> recipients = recipientMemberKeys.stream().distinct().toList();
        notificationWriter.addAll(recipients, eventType, title, body, dataPayload);

        List<FcmDeviceTokenInfo> tokens = fcmDeviceTokenReader.findGuardianTokens(recipients);
        dispatch(tokens, FcmPushMessage.dataOnly(title, body, dataPayload));
    }

    public void sendToMember(String memberKey, String eventType, String title, String body, Map<String, String> dataPayload) {
        notificationWriter.addAll(List.of(memberKey), eventType, title, body, dataPayload);

        List<FcmDeviceTokenInfo> tokens = fcmDeviceTokenReader.findTokensByMemberKey(memberKey);
        dispatch(tokens, FcmPushMessage.notification(title, body, dataPayload));
    }

    private void dispatch(List<FcmDeviceTokenInfo> tokens, FcmPushMessage message) {
        if (tokens.isEmpty()) {
            return;
        }

        List<String> invalidTokens = fcmClient.sendAndCollectInvalidTokens(
                message,
                tokens.stream().map(FcmDeviceTokenInfo::token).toList()
        );
        fcmDeviceTokenManager.deleteInvalidTokens(invalidTokens);
    }
}
