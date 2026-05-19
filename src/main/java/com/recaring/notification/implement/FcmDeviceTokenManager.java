package com.recaring.notification.implement;

import com.recaring.notification.business.command.UpsertFcmDeviceTokenCommand;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class FcmDeviceTokenManager {

    private static final String UNREGISTERED_REASON = "FCM_UNREGISTERED";

    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;

    @Transactional
    public FcmDeviceToken upsert(UpsertFcmDeviceTokenCommand command) {
        validate(command);
        return fcmDeviceTokenRepository.findByToken(command.token())
                .map(existing -> {
                    existing.assignTo(command.memberKey(), command.recipientType(), command.platform());
                    return existing;
                })
                .orElseGet(() -> fcmDeviceTokenRepository.save(FcmDeviceToken.builder()
                        .memberKey(command.memberKey())
                        .recipientType(command.recipientType())
                        .token(command.token())
                        .platform(command.platform())
                        .build()));
    }

    @Transactional
    public void deactivateInvalidTokens(Collection<String> tokens) {
        tokens.forEach(token -> fcmDeviceTokenRepository.findByToken(token)
                .ifPresent(deviceToken -> deviceToken.deactivate(UNREGISTERED_REASON)));
    }

    @Transactional
    public void touchLastUsedAt(Collection<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        fcmDeviceTokenRepository.findAllByTokenIn(tokens)
                .forEach(FcmDeviceToken::touchLastUsedAt);
    }

    private void validate(UpsertFcmDeviceTokenCommand command) {
        if (command.token() == null || command.token().isBlank()) {
            throw new AppException(ErrorType.INVALID_FCM_DEVICE_TOKEN);
        }
        if (command.memberKey() == null || command.memberKey().isBlank() || command.recipientType() == null) {
            throw new AppException(ErrorType.INVALID_FCM_DEVICE_TOKEN);
        }
    }
}
