package com.recaring.device.implement;

import com.recaring.device.dataaccess.entity.WardDeviceToken;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WardDeviceTokenManager {

    private final WardDeviceTokenRepository wardDeviceTokenRepository;
    private final WardDeviceTokenReader wardDeviceTokenReader;

    @Transactional
    public String issueToken(String wardKey) {
        return wardDeviceTokenRepository.findByWardKey(wardKey)
                .map(existing -> {
                    String oldToken = existing.getToken();
                    existing.reissue();
                    wardDeviceTokenReader.evict(oldToken);
                    return existing.getToken();
                })
                .orElseGet(() -> {
                    WardDeviceToken newToken = WardDeviceToken.builder()
                            .wardKey(wardKey)
                            .build();
                    wardDeviceTokenRepository.save(newToken);
                    return newToken.getToken();
                });
    }
}
