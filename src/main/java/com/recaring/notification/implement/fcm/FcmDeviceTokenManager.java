package com.recaring.notification.implement.fcm;

import com.recaring.care.vo.CarePartyRole;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import com.recaring.notification.vo.FcmDeviceTokenInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class FcmDeviceTokenManager {

    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;

    @Transactional
    public FcmDeviceTokenInfo upsert(String memberKey, String token, CarePartyRole careRole, FcmDevicePlatform platform) {
        FcmDeviceToken saved = fcmDeviceTokenRepository.findByToken(token)
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
        return FcmDeviceTokenInfo.from(saved);
    }

    @Transactional
    public void deleteByToken(String token) {
        fcmDeviceTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void deleteInvalidTokens(Collection<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        fcmDeviceTokenRepository.deleteByTokenIn(tokens);
    }

}
