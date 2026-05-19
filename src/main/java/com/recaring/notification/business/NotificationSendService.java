package com.recaring.notification.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.notification.business.command.NotificationSendCommand;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.entity.NotificationDelivery;
import com.recaring.notification.implement.FcmClient;
import com.recaring.notification.implement.FcmDeviceTokenManager;
import com.recaring.notification.implement.FcmDeviceTokenReader;
import com.recaring.notification.implement.FcmPushMessage;
import com.recaring.notification.implement.FcmSendResult;
import com.recaring.notification.implement.NotificationDeliveryManager;
import com.recaring.notification.implement.NotificationWriter;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationSendService {

    private final ObjectMapper objectMapper;
    private final FcmDeviceTokenReader fcmDeviceTokenReader;
    private final FcmDeviceTokenManager fcmDeviceTokenManager;
    private final NotificationWriter notificationWriter;
    private final NotificationDeliveryManager notificationDeliveryManager;
    private final FcmClient fcmClient;

    @Transactional
    public NotificationSendResult send(NotificationSendCommand command) {
        String dataPayloadJson = serialize(command.dataPayload());
        Notification notification = notificationWriter.addRequested(
                command.eventType(),
                command.title(),
                command.body(),
                dataPayloadJson
        );

        List<FcmDeviceToken> tokens = fcmDeviceTokenReader.findActiveRecipientTokens(
                command.guardianMemberKeys(),
                command.managerMemberKeys()
        );
        if (tokens.isEmpty()) {
            notificationWriter.markFailedForNoActiveToken(notification);
            return NotificationSendResult.from(notification, List.of());
        }

        List<NotificationDelivery> deliveries = notificationDeliveryManager.addRequested(notification, tokens);
        FcmSendResult sendResult = fcmClient.send(
                new FcmPushMessage(command.title(), command.body(), command.dataPayload()),
                tokens.stream().map(FcmDeviceToken::getToken).toList()
        );

        notificationDeliveryManager.applyResults(deliveries, sendResult);
        fcmDeviceTokenManager.deactivateInvalidTokens(sendResult.invalidTokens());
        tokens.forEach(FcmDeviceToken::touchLastUsedAt);
        notificationWriter.completeByDeliveries(notification, deliveries);

        return NotificationSendResult.from(notification, deliveries);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorType.NOTIFICATION_PAYLOAD_SERIALIZATION_FAILED);
        }
    }
}
