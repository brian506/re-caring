package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.NotificationRecipientType;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FcmDeviceTokenReader {

    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;

    public List<FcmDeviceToken> findActiveRecipientTokens(
            Collection<String> guardianMemberKeys,
            Collection<String> managerMemberKeys
    ) {
        List<FcmDeviceToken> tokens = new ArrayList<>();
        if (guardianMemberKeys != null && !guardianMemberKeys.isEmpty()) {
            tokens.addAll(fcmDeviceTokenRepository.findAllByMemberKeyInAndRecipientTypeAndActiveTrue(
                    guardianMemberKeys,
                    NotificationRecipientType.GUARDIAN
            ));
        }
        if (managerMemberKeys != null && !managerMemberKeys.isEmpty()) {
            tokens.addAll(fcmDeviceTokenRepository.findAllByMemberKeyInAndRecipientTypeAndActiveTrue(
                    managerMemberKeys,
                    NotificationRecipientType.MANAGER
            ));
        }

        Map<String, FcmDeviceToken> deduplicated = new LinkedHashMap<>();
        tokens.forEach(token -> deduplicated.putIfAbsent(token.getToken(), token));
        return List.copyOf(deduplicated.values());
    }
}
