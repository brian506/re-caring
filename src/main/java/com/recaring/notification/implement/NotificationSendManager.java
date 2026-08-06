package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.implement.fcm.FcmClient;
import com.recaring.notification.implement.fcm.FcmDeviceTokenManager;
import com.recaring.notification.implement.fcm.FcmDeviceTokenReader;
import com.recaring.notification.implement.fcm.FcmPushMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSendManager {

    private final FcmDeviceTokenReader fcmDeviceTokenReader;
    private final FcmDeviceTokenManager fcmDeviceTokenManager;
    private final NotificationWriter notificationWriter;
    private final FcmClient fcmClient;

    public void sendToCareParties(
            Collection<String> guardianMemberKeys,
            Collection<String> managerMemberKeys,
            String eventType,
            String title,
            String body,
            Map<String, String> dataPayload
    ) {
        List<String> recipientMemberKeys = Stream.concat(guardianMemberKeys.stream(), managerMemberKeys.stream())
                .distinct()
                .toList();
        notificationWriter.addAll(recipientMemberKeys, eventType, title, body, dataPayload);

        List<FcmDeviceToken> tokens = fcmDeviceTokenReader.findTokensByCareRoles(guardianMemberKeys, managerMemberKeys);
        dispatch(tokens, title, body, dataPayload);
    }

    public void sendToMember(String memberKey, String eventType, String title, String body, Map<String, String> dataPayload) {
        notificationWriter.addAll(List.of(memberKey), eventType, title, body, dataPayload);

        List<FcmDeviceToken> tokens = fcmDeviceTokenReader.findTokensByMemberKey(memberKey);
        dispatch(tokens, title, body, dataPayload);
    }

    private void dispatch(List<FcmDeviceToken> tokens, String title, String body, Map<String, String> dataPayload) {
        if (tokens.isEmpty()) {
            return;
        }

        List<String> invalidTokens = fcmClient.sendAndCollectInvalidTokens(
                new FcmPushMessage(title, body, dataPayload),
                tokens.stream().map(FcmDeviceToken::getToken).toList()
        );
        fcmDeviceTokenManager.deleteInvalidTokens(invalidTokens);
    }
}
