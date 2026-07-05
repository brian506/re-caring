package com.recaring.notification.implement;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class FcmDeviceTokenManager {

    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;

    @Transactional
    public FcmDeviceToken upsert(String memberKey, String token, CareRole careRole, FcmDevicePlatform platform) {
        return fcmDeviceTokenRepository.findByToken(token)
                .map(existing -> {
                    existing.assignTo(memberKey, careRole, platform);
                    return existing;
                })
                .orElseGet(() -> fcmDeviceTokenRepository.save(FcmDeviceToken.builder()
                        .memberKey(memberKey)
                        .careRole(careRole)
                        .token(token)
                        .platform(platform)
                        .build()));
    }

    @Transactional
    public void deleteInvalidTokens(Collection<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        fcmDeviceTokenRepository.deleteByTokenIn(tokens);
    }

}
